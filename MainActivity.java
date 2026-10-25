package ru.boardmeter.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
  private final ArrayList<JSONObject> entries = new ArrayList<>();
  private final ArrayList<JSONObject> archive = new ArrayList<>();
  private EditText lengthInput, countInput;
  private TextView resultText, totalText, entriesText, shiftText;
  private LinearLayout rows, archiveRows;
  private int shift = 1;
  private final DecimalFormat df = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(new Locale("ru", "RU")));
  private int dp(int x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
  private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(35,43,51));t.setPadding(dp(2),dp(7),dp(2),dp(7));return t;}
  private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  private Button button(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);return b;}
  private void save(){try {JSONObject root=new JSONObject();root.put("shift",shift);root.put("entries",new JSONArray(entries));root.put("archive",new JSONArray(archive));getPreferences(0).edit().putString("data",root.toString()).apply();}catch(Exception ignored){}}
  private void load(){try{JSONObject root=new JSONObject(getPreferences(0).getString("data","{}"));shift=root.optInt("shift",1);JSONArray e=root.optJSONArray("entries");if(e!=null)for(int i=0;i<e.length();i++)entries.add(e.getJSONObject(i));JSONArray a=root.optJSONArray("archive");if(a!=null)for(int i=0;i<a.length();i++)archive.add(a.getJSONObject(i));}catch(Exception ignored){}}
  private long length(){try{return Long.parseLong(lengthInput.getText().toString().trim());}catch(Exception e){return 0;}}
  private long qty(){try{return Long.parseLong(countInput.getText().toString().trim());}catch(Exception e){return 0;}}
  private String meters(long mmPieces){return df.format(mmPieces/1000.0)+" пог. м";}
  private long sum(){long s=0;for(JSONObject o:entries)s+=o.optLong("mm")*o.optLong("qty");return s;}
  private long pieces(){long s=0;for(JSONObject o:entries)s+=o.optLong("qty");return s;}
  private void updatePreview(){long mm=length(),q=qty();resultText.setText(mm>0&&q>0&&mm<=Long.MAX_VALUE/q?meters(mm*q):"0 пог. м");}
  @Override public void onCreate(Bundle b){super.onCreate(b);load();render();}
  private void render(){
    ScrollView scroll=new ScrollView(this);LinearLayout main=column();main.setPadding(dp(16),dp(12),dp(16),dp(30));scroll.addView(main);setContentView(scroll);
    main.addView(label("КАЛЬКУЛЯТОР ДОСОК",24));shiftText=label("Смена №"+shift,17);main.addView(shiftText);
    main.addView(label("Длина доски, мм",16));lengthInput=new EditText(this);lengthInput.setSingleLine(true);lengthInput.setInputType(InputType.TYPE_CLASS_NUMBER);lengthInput.setHint("Например, 3500");main.addView(lengthInput);
    main.addView(label("Количество, шт.",16));countInput=new EditText(this);countInput.setSingleLine(true);countInput.setInputType(InputType.TYPE_CLASS_NUMBER);countInput.setHint("Например, 12");main.addView(countInput);
    resultText=label("0 пог. м",28);main.addView(resultText);
    android.text.TextWatcher watcher=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){updatePreview();}public void afterTextChanged(android.text.Editable e){}};
    lengthInput.addTextChangedListener(watcher);countInput.addTextChangedListener(watcher);
    Button add=button("+ ЗАПОМНИТЬ");main.addView(add);add.setOnClickListener(v->{long mm=length(),q=qty();if(mm<=0||q<=0||mm>Long.MAX_VALUE/q){Toast.makeText(this,"Введите правильные числа",Toast.LENGTH_SHORT).show();return;}try{entries.add(new JSONObject().put("mm",mm).put("qty",q));save();countInput.setText("");refresh();}catch(Exception ignored){}});
    Button clear=button("СТЕРЕТЬ ПОЛЯ");main.addView(clear);clear.setOnClickListener(v->{lengthInput.setText("");countInput.setText("");});
    main.addView(label("РАСЧЁТЫ ЗА СМЕНУ",20));rows=column();main.addView(rows);
    totalText=label("",22);main.addView(totalText);
    Button finish=button("ЗАВЕРШИТЬ СМЕНУ");main.addView(finish);finish.setOnClickListener(v->{if(entries.isEmpty()){Toast.makeText(this,"Смена пуста",Toast.LENGTH_SHORT).show();return;}new AlertDialog.Builder(this).setTitle("Завершить смену?").setMessage("Все записи будут сохранены в архив.").setNegativeButton("Отмена",null).setPositiveButton("Сохранить",(d,w)->{try{JSONObject a=new JSONObject();a.put("number",shift);a.put("date",new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(new Date()));a.put("items",new JSONArray(entries));a.put("sum",sum());a.put("pieces",pieces());archive.add(0,a);entries.clear();shift++;save();refresh();}catch(Exception ignored){}}).show();});
    main.addView(label("АРХИВ СМЕН",20));archiveRows=column();main.addView(archiveRows);
    refresh();
  }
  private void refresh(){rows.removeAllViews();for(int i=0;i<entries.size();i++){final int index=i;JSONObject o=entries.get(i);LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView t=label((i+1)+". "+o.optLong("mm")+" мм × "+o.optLong("qty")+" = "+meters(o.optLong("mm")*o.optLong("qty")),15);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));Button del=button("✕");r.addView(del);del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Удалить запись?").setNegativeButton("Нет",null).setPositiveButton("Удалить",(d,w)->{entries.remove(index);save();refresh();}).show());rows.addView(r);}
    totalText.setText("ИТОГО: "+meters(sum())+"\nДосок: "+pieces()+" шт.");shiftText.setText("Смена №"+shift);archiveRows.removeAllViews();if(archive.isEmpty())archiveRows.addView(label("Архив пока пуст",15));
    for(JSONObject a:archive){String title="Смена №"+a.optInt("number")+" · "+a.optString("date")+"\n"+meters(a.optLong("sum"))+" · "+a.optLong("pieces")+" шт.";Button show=button(title+"  ▾");archiveRows.addView(show);show.setOnClickListener(v->{JSONArray arr=a.optJSONArray("items");StringBuilder detail=new StringBuilder();if(arr!=null)for(int j=0;j<arr.length();j++){JSONObject o=arr.optJSONObject(j);if(o!=null)detail.append(j+1).append(". ").append(o.optLong("mm")).append(" мм × ").append(o.optLong("qty")).append(" = ").append(meters(o.optLong("mm")*o.optLong("qty"))).append("\n");}detail.append("\nИтого: ").append(meters(a.optLong("sum")));new AlertDialog.Builder(this).setTitle("Смена №"+a.optInt("number")).setMessage(detail.toString()).setPositiveButton("Закрыть",null).setNeutralButton("Поделиться",(d,w)->{Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,title+"\n"+detail);startActivity(Intent.createChooser(send,"Отчёт о смене"));}).show();});}
  }
}
