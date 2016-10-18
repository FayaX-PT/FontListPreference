package fayax.me.pt.fontpreferenceslistpicker.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import fayax.me.pt.fontpreferenceslistpicker.R;
import fayax.me.pt.fontpreferenceslistpicker.domain_model.FontFileReader;
import fayax.me.pt.fontpreferenceslistpicker.domain_model.TTFFile;


/**
 * Created by nunol on 27/09/2016.
 */

public class FontListPreference extends ListPreference {
    public static final String MONOSPACE_ENTRY = "Monospace";
    public static final String SANS_SERIF_ENTRY = "Sans Serif";
    public static final String SERIF_ENTRY = "Serif";
    public static final String MONOSPACE_ENTRY_VALUE = "monospace";
    public static final String SANS_SERIF_ENTRY_VALUE = "sans_serif";
    public static final String SERIF_ENTRY_VALUE = "serif";
    public static final String SUMMARY = "font_summary";

    private final ArrayList<RadioButton> radioButtonList;
    private final FontListPreference fontListPreference;
    private final Context context;
    private String path;
    private String defaultValue;
    private final String key;
    private ArrayList<String> entries;
    private ArrayList<String> entryValues;
    private boolean includeSystemFonts;
    private boolean shouldUpdate;
    private final SharedPreferences sPreferences;

    public FontListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        radioButtonList = new ArrayList<>();
        fontListPreference = this;

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FontPreferenceListPicker);
        path = typedArray.getString(R.styleable.FontPreferenceListPicker_path);
        defaultValue = typedArray.getString(R.styleable.FontPreferenceListPicker_defaultValue);
        includeSystemFonts = typedArray.getBoolean(R.styleable.FontPreferenceListPicker_includeSystemFonts, true);
        key = getKey();
        typedArray.recycle();

        shouldUpdate = true;
        entries = new ArrayList<>();
        entryValues = new ArrayList<>();

        sPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sPreferences.registerOnSharedPreferenceChangeListener(spChanged);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
       // getSharedPreferences().registerOnSharedPreferenceChangeListener(spChanged);
        if (entries.size() == 0 || entryValues.size() == 0 || entries.size() != entryValues.size() || shouldUpdate) {
            final AssetManager assetManager = context.getAssets();
            String pathToFontInAssets = null;
            InputStream inputStream = null;
            TTFFile ttfFile = null;
            String fontName = null;

            if (includeSystemFonts) {
                entries.add(MONOSPACE_ENTRY);
                entryValues.add(MONOSPACE_ENTRY_VALUE);
                entries.add(SANS_SERIF_ENTRY);
                entryValues.add(SANS_SERIF_ENTRY_VALUE);
                entries.add(SERIF_ENTRY);
                entryValues.add(SERIF_ENTRY_VALUE);
            }


            try {
                for (String s : assetManager.list(path)) {
                    pathToFontInAssets = path + "/" + s;
                    inputStream = assetManager.open(pathToFontInAssets);
                    ttfFile = FontFileReader.readTTF(inputStream);
                    fontName = ttfFile.getFullName();
                    entries.add(fontName);
                    entryValues.add(pathToFontInAssets);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (entries.size() == 0 || entryValues.size() == 0 || entries.size() != entryValues.size()) {
                throw new IllegalStateException("ListPreference requires an entries array and an entryValues array which are both the same length");
            }
            shouldUpdate = false;
        }

        this.setEntries(entries.toArray(new CharSequence[entries.size()]));
        this.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

        FontAdapter fontAdapter = new FontAdapter();
        builder.setAdapter(fontAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        }).setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getDialog().dismiss();
            }
        });
    }


    public String getDefaultValue() {
        return defaultValue;
    }

    public String getPath() {
        return path;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setValues(ArrayList<String> entries, ArrayList<String> entryValues) {
        this.entries = entries;
        this.entryValues = entryValues;
        this.shouldUpdate = true;
    }

    public void setIncludeSystemFonts(boolean includeSystemFonts) {
        this.includeSystemFonts = includeSystemFonts;
    }

    public void setSummaryByEntryValue(String currentValue) {
        setSummary(getEntryByEntryValue(currentValue));
    }

    private String getEntryByEntryValue(String entryValue) {
        for (int i = 0; i < entryValues.size(); i++) {
            if (entryValues.get(i).equals(entryValue)) {
                return entries.get(i);
            }
        }
        return "";
    }


    final SharedPreferences.OnSharedPreferenceChangeListener spChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefKey) {
            if(prefKey.equals(key)){
                sharedPreferences.edit().putString(SUMMARY, getEntryByEntryValue(sharedPreferences.getString(key, ""))).apply();
            }
        }
    };

    public void setActualSummary(){
        this.setSummary(getSharedPreferences().getString(SUMMARY,""));
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(spChanged);
    }

    private class FontAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private RowHolder holder;

        public FontAdapter() {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(final int position, View row, ViewGroup parent) {
            if (row == null) {
                row = inflater.inflate(R.layout.font_list_preference_item_layout, parent, false);
                holder = new RowHolder(row);
                row.setTag(holder);
            } else {
                holder = (RowHolder) row.getTag();
            }

            holder.getTextView().setText(entries.get(position));
            holder.getTextView().setTextColor(Color.BLACK);
            Typeface font;
            switch (entryValues.get(position)) {
                case MONOSPACE_ENTRY_VALUE:
                    font = Typeface.MONOSPACE;
                    break;
                case SANS_SERIF_ENTRY_VALUE:
                    font = Typeface.SANS_SERIF;
                    break;
                case SERIF_ENTRY_VALUE:
                    font = Typeface.SERIF;
                    break;
                default:
                    font = Typeface.createFromAsset(context.getAssets(), entryValues.get(position));
            }

            holder.getTextView().setTypeface(font);

            if(!radioButtonList.contains(holder.getRadioButton()))
                radioButtonList.add(holder.getRadioButton());
            holder.getRadioButton().setId(position);

            if (getSharedPreferences().getString(key, defaultValue).equals(entryValues.get(position))) {
                holder.getRadioButton().setChecked(true);
            } else {
                holder.getRadioButton().setChecked(false);
            }

            //row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    for (RadioButton rb : radioButtonList) {
                        if (rb.getId() != position)
                            rb.setChecked(false);
                    }
                    fontListPreference.setSummary(entries.get(position));
                    getSharedPreferences().edit().putString(key, entryValues.get(position)).apply();
                    getDialog().dismiss();
                }
            });

            return row;
        }





        private class RowHolder {
            private RadioButton radioButton;
            private TextView textView;
            private View row;

            public RowHolder(View row) {
                this.row = row;
            }

            public RadioButton getRadioButton() {
                if(radioButton == null) {
                    radioButton = (RadioButton) row.findViewById(R.id.custom_list_view_row_radio_button);
                    radioButton.setFocusable(false);
                    radioButton.setClickable(false);
                }
                return radioButton;
            }

            public TextView getTextView() {
                if(textView == null) {
                    textView = (TextView) row.findViewById(R.id.custom_list_view_row_text_view);
                    textView.setFocusable(false);
                }
                return textView;
            }
        }
    }
}
