package dh.sunicon;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import dh.sunicon.datamodel.DatabaseHelper;

public class MainActivity extends ListActivity 
{
	static final String TAG = MainActivity.class.getName();
	private DatabaseHelper dbHelper_;
	private TextView categoryLabel_;
	private ViewSwitcher baseValueSwitcher_;
	private EditText baseValueEditor_;
	private Spinner baseValueSpinner_;
	private AutoCompleteTextView baseUnitEditor_;
	private EditText targetUnitFilterEditor_; 
	private Timer baseValueEditorTimer_;

	private ResultListAdapter resultListAdapter_;
	private SimpleCursorAdapter baseValueSpinnerAdapter_;
	
	private long baseUnitId_ = -1;
	private long categoryId_ = -1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dbHelper_ = new DatabaseHelper(this);
		setContentView(R.layout.sunicon_main);
		
		categoryLabel_ = (TextView)findViewById(R.id.categoryLabel);
		baseValueSwitcher_ = (ViewSwitcher)findViewById(R.id.baseValueSwitcher);
		
		initBaseValueEditor();
        initBaseValueSpinner();
        initBaseUnitAutoCompleteEditor();
        initTargetUnitFilterEditor();
        initResultList();
        initResultListShowHideAnimation();
        clearBaseUnit(false);
	}

	private void initBaseValueEditor()
	{
		baseValueEditor_= (EditText)findViewById(R.id.valueEditor);
		baseValueEditor_.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(final CharSequence s, int start, int before, int count)
			{
				try
				{
					setResultListVisible(false); //hide the list while waiting and perform calculation
					
					/* events absorber technique */
					
					if (baseValueEditorTimer_!=null) 
					{
						baseValueEditorTimer_.cancel(); //cancel the old onTextChange event
					}
	
					//the timer is dumped, we must to create a new one
					baseValueEditorTimer_ = new Timer();
					
					//schedule a task which will be execute in 500ms if the timer won't canceled due 
					//to other (possible future) onTextChanged event
					baseValueEditorTimer_.schedule(new TimerTask()  
					{
						@Override
						public void run()
						{
							MainActivity.this.runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									try
									{
										/*
										 * do whatever onTextChanged event have to do. But it should be quick 
										 * heavy process must be executed on other thread  
										 */
										
										if (TextUtils.isEmpty(s))
										{
											getResultListAdapter().setBaseValue(Double.NaN);
										}
										else
										{
											double baseValue = Double.parseDouble(s.toString());
											getResultListAdapter().setBaseValue(baseValue);
										}
									}
									catch (Exception ex)
									{
										Log.w(TAG, ex);
									}
								}
							});
						}
					}, 500);
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex.toString());
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});
	}

	private void initBaseValueSpinner()
	{
	//		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
	//        R.array.planets_array, android.R.layout.simple_spinner_item);
	//
	//// Specify the layout to use when the list of choices appears
	//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	
		baseValueSpinner_ = (Spinner)findViewById(R.id.valueSpinner);
		baseValueSpinnerAdapter_ = new SimpleCursorAdapter(this, 
				R.layout.spinner_item, 
				null, 
				new String[]{"v"}, ///the query of enumValues must return a column named "v" 
				new int[]{R.id.spinnerLabelItem}, 
				0);
		baseValueSpinner_.setAdapter(baseValueSpinnerAdapter_);
		baseValueSpinnerAdapter_.setFilterQueryProvider(new FilterQueryProvider()
		{
			/**
			 * constraint must be the unitId, this function will return the cursor of the enumValues of a unit 
			 */
			@Override
			public Cursor runQuery(CharSequence unitIdStr)
			{
				try
				{
					if (TextUtils.isEmpty(unitIdStr))
					{
						return null;
					}
					
					Cursor cur = dbHelper_.getReadableDatabase().
							query("enumvalue", new String[]{"id as _id", "value as v"}, 
								"unitid=?", 
								new String[] {unitIdStr.toString()}, 
								null, null, "value");

					simulateLongOperation(3, 4);
					
					/* switch the baseValueEditor to spinner or normal editor*/
					if (cur.getCount()>0)
					{
						//switch to the spinner
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								if (baseValueSwitcher_.getNextView() == baseValueSpinner_)
								{
									baseValueSwitcher_.showNext();
								}
							}
						});
					}
					else
					{
						//switch to the editor
						runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								if (baseValueSwitcher_.getNextView() == baseValueEditor_)
								{
									baseValueSwitcher_.showNext();
								}
							}
						});
					}
					
					return cur;
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
					return null;
				}
			}
		});
		
		
		baseValueSpinner_.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
				// TODO Auto-generated method stub
				
			}
        	
		});
	}
	
	private void initBaseUnitAutoCompleteEditor()
	{
		baseUnitEditor_ = (AutoCompleteTextView)findViewById(R.id.baseUnitEditor);
		final String initialQuery = UnitsCursorAdapter.SELECT_QUERY_PART + UnitsCursorAdapter.WHERE1_QUERY_PART + UnitsCursorAdapter.LIMIT_ORDER_QUERY_PART;
		final Cursor initialCursor = dbHelper_.getReadableDatabase().rawQuery(initialQuery, null);
		UnitsCursorAdapter adapter = new UnitsCursorAdapter(this, initialCursor, false);
		
		baseUnitEditor_.setAdapter(adapter);
	    baseUnitEditor_.setThreshold(1);
	    
	    baseUnitEditor_.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)
			{
				try
				{
					if (view == null)
					{
						return;
					}
					UnitsCursorAdapter.SuggestionData baseUnitData = (UnitsCursorAdapter.SuggestionData)view.getTag();
					setBaseUnit(baseUnitData.getCategoryName(), baseUnitData.getUnitName(), baseUnitData.getCategoryId(), baseUnitData.getUnitId());
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}				
			}
		});
	    
	    baseUnitEditor_.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				try
				{
					if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode != KeyEvent.KEYCODE_DPAD_UP && 
							keyCode != KeyEvent.KEYCODE_DPAD_DOWN && keyCode != KeyEvent.KEYCODE_DPAD_LEFT && 
							keyCode != KeyEvent.KEYCODE_DPAD_RIGHT && keyCode != KeyEvent.KEYCODE_ENTER &&
							keyCode != KeyEvent.KEYCODE_TAB)
					{
						clearBaseUnit(true);
					}
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
				return false;
			}
		});
	}

	private void initTargetUnitFilterEditor()
	{
		targetUnitFilterEditor_ = (EditText)findViewById(R.id.targetUnitFilterEditor);
		targetUnitFilterEditor_.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(final CharSequence s, int start, int before, int count)
			{
				try
				{
					getResultListAdapter().getFilter().filter(s);
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});
	}

	private void initResultList()
	{
		resultListAdapter_ = new ResultListAdapter(this);
		setListAdapter(resultListAdapter_);
		registerForContextMenu(getListView());
	}

	private void initResultListShowHideAnimation()
	{
//		ObjectAnimator fadeinAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.fadein);
//		ObjectAnimator fadeoutAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.fadeout);
//		LayoutTransition transitioner = new LayoutTransition();
//		transitioner.setAnimator(LayoutTransition.APPEARING, fadeinAnim);
//		transitioner.setAnimator(LayoutTransition.DISAPPEARING, fadeoutAnim);
//		
//		ViewGroup container = (ViewGroup)this.getWindow().getDecorView().findViewById(R.id.mainView);
//		container.setLayoutTransition(transitioner);		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		try
		{
			if (v == getListView())
			{
				MenuInflater inflater = getMenuInflater();
	            inflater.inflate(R.menu.result_list_contextmenu, menu);
	            
	            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	            RowData rowData = (RowData)getListAdapter().getItem(info.position);
	            menu.setHeaderTitle(rowData.getUnitName());
			}
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}	
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		try
		{
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			RowData rowData = (RowData)getListAdapter().getItem(info.position);
		    switch (item.getItemId()) 
		    {
		        case R.id.copyValueItem:
		        	Toast.makeText(this, "copyValueItem "+rowData.getValue(), Toast.LENGTH_SHORT).show();
		            return true;
		        case R.id.copyUnitNameItem:
		        	Toast.makeText(this, "copyUnitNameItem "+rowData.getUnitName(), Toast.LENGTH_SHORT).show();
		            return true;
		        case R.id.copyValueAndUnitItem:
		        	Toast.makeText(this, "copyValueAndUnitItem", Toast.LENGTH_SHORT).show();
		            return true;
		        case R.id.setValueAsBaseItem:
		        	Toast.makeText(this, "setValueAsBaseItem", Toast.LENGTH_SHORT).show();
		            return true;
		        case R.id.setUnitAsBaseItem:
		        	Toast.makeText(this, "setUnitAsBaseItem", Toast.LENGTH_SHORT).show();
		            return true;
		        case R.id.setUnitAndValueAsBaseItem:
		        	Toast.makeText(this, "setUnitAndValueAsBaseItem", Toast.LENGTH_SHORT).show();
		            return true;
		    }
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
		}
		return super.onContextItemSelected(item);
	}
	
	public void setBaseUnit(CharSequence categoryName, CharSequence unitName, long categoryId, long unitId)
	{
		if (categoryId == -1 && unitId == -1)
		{
			clearBaseUnit(false);
			return;
		}
		categoryLabel_.setVisibility(View.VISIBLE);
		categoryLabel_.setText(categoryName);
		categoryId_ = categoryId;
		baseUnitId_ = unitId;
		targetUnitFilterEditor_.setEnabled(true);
		baseValueSpinnerAdapter_.getFilter().filter(Long.toString(unitId));
		
		try
		{
			getResultListAdapter().setBaseUnitId(categoryId_, baseUnitId_);
		}
		catch (IllegalAccessException e)
		{
			Log.w(TAG, e);
		}
	}
	
	public void clearTargetUnitFilterButton_Click(View v)
	{
		targetUnitFilterEditor_.setText(null);
	}

	private void clearBaseUnit(boolean keepTextOnBaseUnitEditor)
	{
		categoryLabel_.setVisibility(View.GONE);
		categoryId_ = -1;
		baseUnitId_ = -1;
		targetUnitFilterEditor_.setEnabled(false);
		if (!keepTextOnBaseUnitEditor)
		{
			baseUnitEditor_.setText(null);
		}
	}
	
	public void clearBaseUnitButton_Click(View v)
	{
		clearBaseUnit(false);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_lesson_one, menu);
		return true;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putCharSequence("categoryName", categoryLabel_.getText());
		outState.putCharSequence("baseUnitName", baseUnitEditor_.getText());
		outState.putLong("categoryId", categoryId_);
		outState.putLong("baseUnitId", baseUnitId_);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state)
	{
		super.onRestoreInstanceState(state);
		setBaseUnit(
				state.getCharSequence("categoryName"), 
				state.getCharSequence("baseUnitName"),
				state.getLong("categoryId"),
				state.getLong("baseUnitId"));
	}
	
	
	public DatabaseHelper getDatabaseHelper(){
		return dbHelper_;
	}
	
	/**
	 * Simulate a thread of long operation
	 * @param minSecond
	 * @param maxSecond
	 */
	public static void simulateLongOperation(int minSecond, int maxSecond)
	{
		Random rand = new Random(System.currentTimeMillis());
		long timeToSleep = (rand.nextInt(maxSecond-minSecond)+minSecond)*1000;
		
		try
		{
			Thread.sleep(timeToSleep);
		}
		catch (InterruptedException e)
		{
			Log.w("SimulationQuery", e);
		}
	}

	public void setResultListVisible(boolean visible)
	{
		getListView().setVisibility(visible ? View.VISIBLE : View.GONE);
	}
	
	ResultListAdapter getResultListAdapter()
	{
		return resultListAdapter_;
	}
	
}
