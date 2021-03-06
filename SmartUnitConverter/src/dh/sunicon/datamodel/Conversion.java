package dh.sunicon.datamodel;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

public class Conversion extends BaseEntity
{
	private static final String TAG = Conversion.class.getName();
	private long base_;
	private long target_;
	private Double fx_; // target = fx*base
	private String formula_; // target = sqrt(%base)
	private String reversedFormula_; // base = %target^2

	public Conversion(DatabaseHelper dbHelper, long id, long base, long target,
			Double fx, String formula, String reversedFormula)
	{
		super(dbHelper, id);
		this.base_ = base;
		this.target_ = target;
		this.fx_ = fx;
		this.formula_ = formula;
		this.reversedFormula_ = reversedFormula;
	}
	
	public Conversion(DatabaseHelper dbHelper, JSONObject jsonData) throws JSONException
	{
		this(dbHelper, jsonData.getLong("id")
				, jsonData.getLong("base")
				, jsonData.getLong("target")
				, jsonData.has("fx") ? jsonData.getDouble("fx") : null
				, jsonData.getString("formula")
				, jsonData.getString("reversedFormula"));
	}

	public JSONObject serialize() throws JSONException
	{
		JSONObject json = new JSONObject();
		
		json.put("id", getId());
		json.put("base", base_);
		json.put("target", target_);
		json.put("fx", fx_);
		json.put("formula", formula_);
		json.put("reversedFormula", reversedFormula_);
		
		return json;
	}
	
	public long getBaseUnitId()
	{
		return base_;
	}

	public long getTargetUnitId()
	{
		return target_;
	}

	public Double getFx()
	{
		return fx_;
	}

	public String getFormula()
	{
		return formula_;
	}

	public String getReversedFormula()
	{
		return reversedFormula_;
	}

	public Unit getBaseUnit()
	{
		return Unit.findById(this.getDbHelper(), this.getBaseUnitId());
	}

	public Unit getTargetUnit()
	{
		return Unit.findById(this.getDbHelper(), this.getTargetUnitId());
	}

	/**
	 * return true if the conversion is a link unitId to other_unit_id
	 * and we can use this conversion to convert other_unit to uniId
	 * 
	 * example Conversion(Km, m) is IncidentEdgeOf(m) if the formula_ is not null or
	 * the fx has an value
	 * 
	 *  this function is used to detect if an unitId is a direct-neigbor of other unitId
	 *  in the computeTargetValue() BFS algorithm
	 */
	public boolean isIncidentEdgeOf(long unitId)
	{
		if (!TextUtils.isEmpty(formula_) && unitId == target_) {
			return true;
		}

		if (!TextUtils.isEmpty(reversedFormula_) && unitId == base_) {
			return true;
		}
		
		if (!Double.isNaN(fx_)) {
			return unitId == target_ || unitId == base_;
		}
		
		return false;
	}
	
	public long getOtherUnitId(long unitId)
	{
		if (unitId == base_)
		{
			return target_;
		}
		if (unitId == target_)
		{
			return base_;
		}
		throw new IllegalArgumentException();
	}
	
	public double convert(double value, long fromUnitId)
	{
		if (!TextUtils.isEmpty(formula_) && fromUnitId == base_) {
			return evalFormula(formula_, value);
		}
		if (!TextUtils.isEmpty(reversedFormula_) && fromUnitId == target_) {
			return evalFormula(reversedFormula_, value);
		}
		if (!Double.isNaN(fx_)) {
			if (fromUnitId == base_)
			{
				return fx_*value;
			}
			if (fromUnitId == target_)
			{
				return value/fx_;
			}
		}	
		
		throw new IllegalArgumentException();
	}

	public double evalFormula(String formula, double value)
	{
		try
		{
			MathEval math = new MathEval();
			math.setVariable("x", value);
			return math.evaluate(formula);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Failed to evaluate expression "+formula, e);
			return Double.NaN;
		}
	}
	
	public static Conversion parseCursor(DatabaseHelper dbHelper, Cursor cur)
	{
		return new Conversion(dbHelper, cur.getLong(cur.getColumnIndex("id")),
				cur.getLong(cur.getColumnIndex("base")), cur.getLong(cur
						.getColumnIndex("target")), cur.getDouble(cur
						.getColumnIndex("fx")), cur.getString(cur
						.getColumnIndex("formula")), cur.getString(cur
						.getColumnIndex("reversedFormula")));
	}
}
