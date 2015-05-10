/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.jclehner.rxdroid.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.jclehner.androidutils.AdvancedDialogPreference;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;

public class DrugNamePreference2 extends BaseAdvancedDialogPreference<String>
{
    private AutoCompleteTextView mEditText;
    private ArrayAdapter<String> mAutoCompleteAdapter;
//	private EditText mEditText;
	private Button mBtnPositive;

	private String mOriginalName;

	public DrugNamePreference2(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	@Override
	public void setTitle(CharSequence title)
	{
		if(title == null || title.equals(""))
			super.setTitle(getContext().getString(R.string._title_drug_name));
		else
			super.setTitle(title);
	}

	@Override
	public CharSequence getSummary() {
		return null;
	}

	@Override
	public CharSequence getDialogTitle() {
		return null;
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		super.onDismiss(dialog);
		mEditText = null;
		mBtnPositive = null;
	}

	@Override
	protected void onValueSet(String value)
	{
		mOriginalName = value;
		setTitle(value);
	}


    private class DoAutoCompleteSearch extends AsyncTask<String, Void, ArrayList<String>> {
        private final String URL = "http://jagannath.pdn.cam.ac.uk:31339/DDIServlet.groovy";
        private String responseJSON;
        Gson gson = new Gson();

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            StringBuilder builder = new StringBuilder();
            ArrayList<String> autoComplete = new ArrayList<String>();
            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(URL+"?label="+params[0]);
            Log.e("Network", "Establishing connection...");
            try {
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String respString = "";
                    String line = new String();
                    while ((line = reader.readLine())!=null) {
                        respString += line ;
                    }
                    System.out.println("Response: "+respString);
                    List<Map<String,Set<String>>> list = null ;
                    List<String> resultlist = new ArrayList<String>() ;
                    try {
                        list = gson.fromJson(respString, new TypeToken<List<Map<String,Set<String>>>>() {
                        }.getType());
                        for (Map<String,Set<String>> m : list) {
                            for (String s : m.keySet()) {
                                resultlist.add(s);
                            }

                        }
                    } catch (Exception E) {
                        System.out.println(E.getMessage());
                    }
                    if (resultlist!=null)
                        System.out.println("List: "+resultlist);
                        autoComplete = new ArrayList(resultlist) ;
                } else {
                    Log.e("JSON", "Failed to download file");
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return autoComplete;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            mAutoCompleteAdapter.clear();
            for (String s : result)
                mAutoCompleteAdapter.add(s);
        }
    }


	@Override
	protected View onCreateDialogView()
	{
		mEditText = new AutoCompleteTextView(getContext());
        mAutoCompleteAdapter = new ArrayAdapter<String>(this.getContext(),android.R.layout.simple_dropdown_item_1line);
        System.out.println("Initializing autocomplete...");
        mEditText.setThreshold(3);
        mEditText.setAdapter(mAutoCompleteAdapter);
        mEditText.addTextChangedListener(new TextWatcher() {
            private boolean shouldAutoComplete = true;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                shouldAutoComplete = true;
                for (int position = 0; position < mAutoCompleteAdapter.getCount(); position++) {
                    if (mAutoCompleteAdapter.getItem(position).equalsIgnoreCase(s.toString())) {
                        shouldAutoComplete = false;
                        break;
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (shouldAutoComplete) {
                    new DoAutoCompleteSearch().execute(s.toString());
                }
            }
        });
		//		mEditText = new EditText(getContext());
		mEditText.setText(getValue());
		mEditText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		//mEditText.setSelectAllOnFocus(true);
		mEditText.addTextChangedListener(mWatcher);
		return mEditText;
	}

	@Override
	protected void onPrepareDialog(Dialog dialog)
	{
		dialog.setOnShowListener(new OnShowListener() {

			@Override
			public void onShow(DialogInterface dialog) {
				mBtnPositive = ((AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
			}
		});
	}

	@Override
	protected int onGetSoftInputMode()
	{
		return WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
	}

	@Override
	protected String getDialogValue() {
		return mEditText.getText().toString();
	}

	@Override
	protected String toPersistedString(String value) {
		return value;
	}

	@Override
	protected String fromPersistedString(String string) {
		return string;
	}

	private boolean isUniqueDrugName(String name)
	{
		for(Drug drug : Database.getAll(Drug.class))
		{
			if(name.equals(drug.getName()))
				return false;
		}

		return true;
	}

	private final TextWatcher mWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void afterTextChanged(Editable s)
		{
			String name = s.toString();
			if(!name.equals(mOriginalName) && !isUniqueDrugName(name))
			{
				mEditText.setError(getContext().getString(R.string._msg_err_non_unique_drug_name));
				mBtnPositive.setEnabled(false);
			}
			else
			{
				mEditText.setError(null);
				mBtnPositive.setEnabled(true);
			}
		}
	};
}
