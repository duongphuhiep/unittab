package dh.sunicon;

import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.TextView;
import dh.sunicon.datamodel.DatabaseHelper;

public class UnitsCursorAdapter extends CursorAdapter implements
		Filterable
{
	static final String TAG = UnitsCursorAdapter.class.getName();
	
	static final String SELECT_QUERY_PART = 
			"SELECT"
			+" unit.id as _id"
			+", unit.name as unitName" 
			+", unit.shortName as unitShortName" 
			+", category.Name as categoryName"
			+", category.Id as categoryId"
			+" FROM unit INNER JOIN category ON unit.categoryId = category.id ";
	
	static final String WHERE1_QUERY_PART = 
			"WHERE (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(categoryName) LIKE ?) ";
	
	static final String WHERE2_QUERY_PART = 
			"AND (lower(unitName) LIKE ? OR  lower(unitShortName) LIKE ? OR lower(categoryName) LIKE ?) ";
	
	/**
	 * Cursor contains 60 rows max 
	 */
	static final String LIMIT_ORDER_QUERY_PART = "ORDER BY unitName LIMIT 60";
	
	/*
	static final long EventsAbsorberLatency = 1000; //milisecond
	static long lastInvokeTime;
	*/
	
	private final LayoutInflater inflater;
	private final DatabaseHelper dbHelper;
	
	public UnitsCursorAdapter(Context context, Cursor c,
			boolean autoRequery)
	{
		super(context, c, autoRequery);
		dbHelper = ((MainActivity)context).getDatabaseHelper();
		inflater = LayoutInflater.from(context);
	}

	public UnitsCursorAdapter(Context context, Cursor c,
			int flags)
	{
		super(context, c, flags);
		dbHelper = ((MainActivity)context).getDatabaseHelper();
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		//get the LinearLayout from the unit_dropdown_item
		View unitDropDownItemView = inflater.inflate(R.layout.unit_dropdown_item, parent, false);
		
		//save children views in the tag to avoid call findViewById
		TextView categoryLabel = (TextView) unitDropDownItemView.findViewById(R.id.categoryLabel);
		TextView unitLabel = (TextView) unitDropDownItemView.findViewById(R.id.unitLabel);
		unitDropDownItemView.setTag(new TextView[] {categoryLabel, unitLabel});
		
		return unitDropDownItemView;
	}

	@Override
    public void bindView(View view, Context context, Cursor cursor) 
	{
		//get children views from tag objects
		TextView[] childrenViews = (TextView[])view.getTag();
		TextView categoryLabel = childrenViews[0];
		TextView unitLabel = childrenViews[1];
		
		///bind data to the dropdown item view
		categoryLabel.setText(cursor.getString(cursor.getColumnIndex("categoryName")));
		unitLabel.setText(cursor.getString(cursor.getColumnIndex("unitName")));
    }

	@Override
	public String convertToString(Cursor cursor)
	{
		// this method dictates what is shown when the user clicks each entry in
		// your autocomplete list
		
		String unitName = cursor.getString(cursor.getColumnIndex("unitName"));
		String categoryName = cursor.getString(cursor.getColumnIndex("categoryName"));
		long unitId = cursor.getLong(cursor.getColumnIndex("_id"));
		long categoryId = cursor.getLong(cursor.getColumnIndex("categoryId"));
		
		return categoryName+'\n'+unitName+'\n'
				+categoryId+'\n'+unitId;
	}
	
	private final int DELAY_RUN_QUERY = 500;
	private Object lockLastConstraint_ = new Object();
	private String lastConstraint_;
	
	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint)
	{
		try
		{
			/* delayer events technique */
			
			if (constraint!=null)
			{
				synchronized (lockLastConstraint_)
				{
					lastConstraint_ = new String(constraint.toString());
				}
			}
			
			Thread.sleep(DELAY_RUN_QUERY);
		
			if (lastConstraint_!=null)
			{
				if (!lastConstraint_.equals(constraint))
				{
					/*
					 * lastConstraint_ has been changed after 500ms 
					 * => other runQueryOnBackgroundThread has been called
					 * => no need to execute this one
					 */ 
					return null; 
				}
			}
			
			/* this is how you query for suggestions */
			
			if (getFilterQueryProvider() != null)
			{
				return getFilterQueryProvider().runQuery(constraint);
			}
			
			// build the query by combining queryPartSelect + queryPartWhere1 (or 2) + queryPartLimit
			
			String wherePart = "";
			LinkedList<String> selectionArgs = null;
			
			if (!TextUtils.isEmpty(constraint))
			{
				String filterText = constraint.toString().trim().toLowerCase();
				selectionArgs = new LinkedList<String>();
				
				final String[] words = filterText.split(" ");
				
				String firstWord = words[0];
				wherePart = WHERE1_QUERY_PART;
				selectionArgs.addLast('%'+firstWord+'%');
				selectionArgs.addLast('%'+firstWord+'%');
				selectionArgs.addLast('%'+firstWord+'%');
				
				final int wordCount = words.length;
				for (int k = 1; k < wordCount; k++)
				{
					String word = words[k];
					if (TextUtils.isEmpty(word))
					{
						continue;
					}
					
					wherePart = wherePart.concat(WHERE2_QUERY_PART);
					selectionArgs.addLast('%'+word+'%');
					selectionArgs.addLast('%'+word+'%');
					selectionArgs.addLast('%'+word+'%');
				}
			}
			
			//MainActivity.simulateLongOperation(1, 3);
			
			final String queryComplete = SELECT_QUERY_PART + wherePart + LIMIT_ORDER_QUERY_PART;
			
			String[] argsArray = null;
			if (selectionArgs != null)
			{
				argsArray = selectionArgs.toArray(new String[selectionArgs.size()]);
			}
			//Log.d(TAG, String.format("%s - args.count = %d", queryComplete, argsArray == null ? 0 : argsArray.length));
			 
			//String[] argsArray = selectionArgs == null ? null : selectionArgs.toArray(new String[]{});
			Cursor c = dbHelper.getReadableDatabase().rawQuery(queryComplete,  argsArray);
			return c;
		}
		catch (Exception ex)
		{
			Log.w(TAG, ex);
			return null;
		}
	}

}
