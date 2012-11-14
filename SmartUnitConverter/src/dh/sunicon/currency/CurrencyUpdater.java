package dh.sunicon.currency;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

/**
 * CurrencyUdpdater is the entry point to call update currencies exchange rates.
 * to start the update from the main thread, call process(baseCurrency) 

 * Capture the BeforeUpdateStarted and OnUpdateFinished on the main thread to update the UI
 * call cancel() to stop the process at anytime. OnUpdateFinish will be callback.
 * 
 * @author hiep
 */
public class CurrencyUpdater
{
	private static final String TAG = CurrencyUpdater.class.getName();
	
	public static int OPT_ALL_NETWORK = 0;
	public static int OPT_WIFI_ONLY = 1;
	public static int OPT_NEVER = 2;
	public static final String CurrencyUpdaterOptionName = "CurrencyUpdaterOption";
	
	private final Activity context_;
	private final SharedPreferences preferences_;
	private final ExecutorService updatingThread_;
	private UpdatingAgentsManager currencyImporter_;
	private long currencyUnitIdOnProcess_ = -1;
	
	public CurrencyUpdater(Activity context){
		context_ = context;
		preferences_ = context_.getPreferences(Activity.MODE_PRIVATE);
		updatingThread_ = Executors.newSingleThreadExecutor();
	}
	
	
	/**
	 * Must be call on the Main Thread
	 * @param currencyUnitId - base Currency, the update will be perform on this base
	 * @throws IllegalAccessException 
	 */
	public void process(final long currencyUnitId) throws IllegalAccessException {
		
		if (Looper.getMainLooper().getThread() != Thread.currentThread())
		{
			throw new IllegalAccessException("This methode must be called from UI Thread.");
		}

		if (currencyUnitId<0) {
			return;
		}
		
		if (currencyUnitId == getCurrencyUnitIdOnLoading()) { //the currencyUnitId is already in processing
			Log.d("CURR", "process of "+currencyUnitId+" is happen");
			return;
		}
		
		final int currencyUpdaterOption = getCurrencyUpdaterOption();
		
		if (currencyUpdaterOption == OPT_NEVER) {
			return;
		}
		
		Log.v("CURR", "Process BEGIN "+currencyUnitId);
		
		if (beforeUpdateStarted_!=null)
			beforeUpdateStarted_.beforeUpdateStarted(CurrencyUpdater.this, currencyUnitId);
		
		class CallableWithParam implements Callable<UpdatingReport>
		{
			private final long currencyUnitId__;
			private final UpdatingAgentsManager currencyImporter__;
			
			public CallableWithParam(long currencyUnitId, UpdatingAgentsManager currencyImporter) {
				currencyUnitId__ = currencyUnitId;
				currencyImporter__ = currencyImporter;
			}
			
			@Override
			public UpdatingReport call() throws Exception
			{
				try
				{
					currencyUnitIdOnProcess_ = currencyUnitId__;
					
					final UpdatingReport ret = currencyImporter__.importOnBackground(currencyUnitId__);
					
					//if this is the last called proccess (the last currencyImporter)
					//it might not neccessary because the currencyUpdater processed on single thread (updatingThread_)
					if (currencyImporter__ == CurrencyUpdater.this.currencyImporter_)   
					{
						currencyUnitIdOnProcess_ = -1;
						context_.runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								try {
									if (onUpdateFinished_!=null)
										onUpdateFinished_.onUpdateFinished(CurrencyUpdater.this, ret);
								}
								catch (Exception ex)
								{
									Log.wtf(TAG, ex);
								}
							}
						});
					}
					
					return ret;
				}
				catch (Exception ex)
				{
					Log.wtf(TAG, ex); ///it cannot happen!
				}
				return null;
			}
		}
		
		//dump old currencyImporter to cancel old update which is currently running on the updatingThread (if there is one)
		if (currencyImporter_ != null) {
			currencyImporter_.dumpIt();
		}
		currencyImporter_ = new UpdatingAgentsManager(context_);
		updatingThread_.submit(new CallableWithParam(currencyUnitId, currencyImporter_));
				
		Log.v("CURR", "Process END "+currencyUnitId);
	}
	
	public void cancel() {
		if (currencyImporter_ != null)
		{
			currencyImporter_.dumpIt();
		}
	}
	
	/** events handling **/
	
	private OnUpdateFinishedListener onUpdateFinished_;
	private BeforeUpdateStartedListener beforeUpdateStarted_;
	
	public void setOnUpdateFinished(OnUpdateFinishedListener listener) {
		onUpdateFinished_ = listener;
	}
	
	public void setBeforeUpdateStarted_(BeforeUpdateStartedListener listener) {
		beforeUpdateStarted_ = listener;
	}

	/**
	 * return -1 if no currency is loading
	 */
	public long getCurrencyUnitIdOnLoading()
	{
		return currencyUnitIdOnProcess_;
	}
	
	private int getCurrencyUpdaterOption() {
		return preferences_.getInt(CurrencyUpdaterOptionName, OPT_ALL_NETWORK);
	}

	public interface OnUpdateFinishedListener {
		void onUpdateFinished(CurrencyUpdater sender, UpdatingReport result);
	}
	
	public interface BeforeUpdateStartedListener {
		void beforeUpdateStarted(CurrencyUpdater sender, long currencyUnitId);
	}
}
