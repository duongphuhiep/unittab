package dh.sunicon.datamodel;

import org.json.JSONException;
import org.json.JSONObject;



public class Corresponding extends BaseEntity
{
	//private static final String TAG = Corresponding.class.getName();
	private long enumid1_;
	private long enumid2_;

	public Corresponding(DatabaseHelper dbHelper, long id, long enumid1, long enumid2)
	{
		super(dbHelper, id);
		this.enumid1_ = enumid1;
		this.enumid2_ = enumid2;
	}

	public Corresponding(DatabaseHelper dbHelper, JSONObject jsonData) throws JSONException
	{
		this(dbHelper, jsonData.getLong("id")
				, jsonData.getLong("enumid1")
				, jsonData.getLong("enumid2"));
	}

	public JSONObject serialize() throws JSONException
	{
		JSONObject json = new JSONObject();
		
		json.put("id", getId());
		json.put("enumid1", enumid1_);
		json.put("enumid2", enumid2_);
		
		return json;
	}
	
	public long getEnumid1()
	{
		return enumid1_;
	}

	public long getEnumid2()
	{
		return enumid2_;
	}
	
	/**
	 * return true if the corresponding is a link between enumId and other_enum_id
	 */
	public boolean isIncidentEdgeOf(long enumId)
	{
		return enumid1_ == enumId || enumid2_ == enumId;
	}
	
	public long getOtherEnumId(long firstEnumId)
	{
		if (firstEnumId == enumid1_)
		{
			return enumid2_;
		}
		if (firstEnumId == enumid2_)
		{
			return enumid1_;
		}
		throw new IllegalArgumentException("the firstEnumId parameter does NOT match one of the enumId holding by this Corresponding");
	}

//	public static Corresponding parseCursor(DatabaseHelper dbHelper, Cursor cur)
//	{
//		return new Corresponding(dbHelper, cur.getLong(cur.getColumnIndex("id")),
//				cur.getLong(cur.getColumnIndex("base")), cur.getLong(cur
//						.getColumnIndex("target")), cur.getDouble(cur
//						.getColumnIndex("fx")), cur.getString(cur
//						.getColumnIndex("formula")), cur.getString(cur
//						.getColumnIndex("reversedFormula")));
//	}
}
