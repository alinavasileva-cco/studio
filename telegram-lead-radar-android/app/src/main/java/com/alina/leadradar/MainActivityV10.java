package com.alina.leadradar;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivityV10 extends Activity {
    private LeadStore store;
    private TextView status, results;
    private CheckBox sites, presentations;
    private EditText channels, profileUrl;
    private Spinner lookback;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() { updateStatus(); updateResults(); uiHandler.postDelayed(this, 1500L); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); store = new LeadStore(this); setTitle("Universal Lead Radar v10"); setContentView(buildUi()); loadSettings();
    }
    @Override protected void onResume(){super.onResume();updateStatus();updateResults();uiHandler.removeCallbacks(autoRefresh);uiHandler.postDelayed(autoRefresh,400L);}
    @Override protected void onPause(){uiHandler.removeCallbacks(autoRefresh);super.onPause();}

    private View buildUi(){
        int pad=dp(18); ScrollView scroll=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);root.setPadding(pad,pad,pad,pad);scroll.addView(root);
        TextView title=new TextView(this);title.setText("Universal Telegram Lead Radar v10");title.setTextSize(24);title.setTypeface(Typeface.DEFAULT_BOLD);root.addView(title);
        TextView subtitle=new TextView(this);subtitle.setText("РУЧНОЙ ПОИСК · СВЕЖИЙ СНИМОК КАЖДЫЙ ЗАПУСК.\nТеперь бот проверяет обычную публичную историю и дополнительно публичный поиск внутри каждого канала по презентациям, PowerPoint, лендингам и сайтам. Большие посты разбираются на отдельные задания. Только прямой Telegram-контакт.");subtitle.setTextSize(15);subtitle.setPadding(0,dp(8),0,dp(14));root.addView(subtitle);
        status=new TextView(this);status.setTextSize(16);status.setPadding(0,0,0,dp(12));root.addView(status);
        root.addView(label("Что искать")); sites=new CheckBox(this);sites.setText("Создание лендинга / простого сайта — НЕ Tilda и НЕ WordPress");root.addView(sites); presentations=new CheckBox(this);presentations.setText("Создание / оформление презентации, PowerPoint / PDF / pitch deck");root.addView(presentations);
        root.addView(label("За какой период полностью перечитать историю")); lookback=new Spinner(this);String[] periods={"24 часа","3 дня","7 дней","14 дней"};ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,periods);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);lookback.setAdapter(a);root.addView(lookback,fullWidth());
        TextView h=new TextView(this);h.setText("По умолчанию 3 дня. Каждый запуск очищает прошлую выдачу и строит новую.");h.setTextSize(13);root.addView(h);
        root.addView(label("Telegram-каналы с заказами")); channels=new EditText(this);channels.setMinLines(10);channels.setGravity(android.view.Gravity.TOP);root.addView(channels,fullWidth());
        root.addView(label("Ссылка на портфолио"));profileUrl=new EditText(this);profileUrl.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);root.addView(profileUrl,fullWidth());
        Button start=new Button(this);start.setText("ЗАПУСТИТЬ СВЕЖИЙ РУЧНОЙ ПОИСК");start.setOnClickListener(v->startManualSearch());root.addView(start,buttonParams());
        Button stop=new Button(this);stop.setText("ОСТАНОВИТЬ ТЕКУЩИЙ ПОИСК");stop.setOnClickListener(v->stopManualSearch());root.addView(stop,buttonParams());
        Button openPost=new Button(this);openPost.setText("ОТКРЫТЬ ИСХОДНЫЙ ПОСТ ПОСЛЕДНЕЙ ЗАЯВКИ");openPost.setOnClickListener(v->openUrl(store.lastPreviewPost(),"Пока нет заявок"));root.addView(openPost,buttonParams());
        Button openContact=new Button(this);openContact.setText("ОТКРЫТЬ TELEGRAM-КОНТАКТ ПОСЛЕДНЕЙ ЗАЯВКИ");openContact.setOnClickListener(v->openUrl(LeadStore.contactUrl(store.lastPreviewUser()),"Пока нет контакта"));root.addView(openContact,buttonParams());
        Button copy=new Button(this);copy.setText("СКОПИРОВАТЬ ОТКЛИК ПОСЛЕДНЕЙ ЗАЯВКИ");copy.setOnClickListener(v->{String m=store.lastPreviewMessage();if(m==null||m.isEmpty()){Toast.makeText(this,"Пока нет отклика",Toast.LENGTH_SHORT).show();return;}((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Отклик",m));Toast.makeText(this,"Отклик скопирован",Toast.LENGTH_SHORT).show();});root.addView(copy,buttonParams());
        Button clear=new Button(this);clear.setText("ОЧИСТИТЬ РЕЗУЛЬТАТЫ");clear.setOnClickListener(v->{if(store.running()){Toast.makeText(this,"Сначала останови поиск",Toast.LENGTH_SHORT).show();return;}store.clearPreviewHistory();updateStatus();updateResults();});root.addView(clear,buttonParams());
        root.addView(label("Найденные заявки текущего запуска"));results=new TextView(this);results.setTextSize(14);results.setTextIsSelectable(true);results.setAutoLinkMask(Linkify.WEB_URLS);results.setMovementMethod(LinkMovementMethod.getInstance());results.setPadding(dp(10),dp(10),dp(10),dp(18));root.addView(results,fullWidth());
        return scroll;
    }

    private void loadSettings(){sites.setChecked(store.sitesEnabled());presentations.setChecked(store.presentationsEnabled());channels.setText(store.channels());profileUrl.setText(store.profileUrl());setLookbackSelection(store.lookbackDays());updateStatus();updateResults();}
    private void saveSettings(){store.setCategories(sites.isChecked(),presentations.isChecked());store.setChannels(channels.getText().toString());store.setProfileUrl(profileUrl.getText().toString());store.setLookbackDays(selectedLookbackDays());}
    private void startManualSearch(){if(store.running()){Toast.makeText(this,"Поиск уже идёт",Toast.LENGTH_SHORT).show();return;}if(!sites.isChecked()&&!presentations.isChecked()){Toast.makeText(this,"Выбери направление",Toast.LENGTH_SHORT).show();return;}saveSettings();requestNotificationsIfNeeded();Intent i=new Intent(this,ScannerService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);Toast.makeText(this,"Свежий поиск v10 запущен",Toast.LENGTH_LONG).show();}
    private void stopManualSearch(){if(!store.running())return;store.requestStop();Intent i=new Intent(this,ScannerService.class);i.setAction(ScannerService.ACTION_STOP);startService(i);}
    private void updateStatus(){if(status==null)return;StringBuilder s=new StringBuilder();s.append("Режим: ТОЛЬКО РУЧНОЙ · АВТООТПРАВКИ НЕТ\n");s.append("Период: ").append(periodLabel(store.lookbackDays())).append("\n");s.append("Поиск: ").append(store.running()?"ИДЁТ":"НЕ ЗАПУЩЕН").append("\n");int total=store.totalChannels();if(total>0)s.append("Проверено источников: ").append(store.checkedChannels()).append(" / ").append(total).append("\n");else s.append("Источников в списке: ").append(countChannels(store.channels())).append("\n");if(store.running()&&!store.currentChannel().isEmpty())s.append("Сейчас: @").append(store.currentChannel()).append("\n");s.append("Реально прочитано постов: ").append(store.diagnosticPosts()).append("\n");s.append("Разобрано блоков/задач: ").append(store.diagnosticBlocks()).append("\n");s.append("Кандидатов по теме: ").append(store.diagnosticCandidates()).append("\n");s.append("Без прямого TG-контакта: ").append(store.diagnosticNoContact()).append("\n");s.append("Найдено подходящих: ").append(store.runFound());status.setText(s.toString());}
    private void updateResults(){if(results==null)return;String h=store.previewHistory();results.setText(h==null||h.isEmpty()?(store.running()?"Идёт поиск…":"Пока подходящих заявок нет."):h);Linkify.addLinks(results,Linkify.WEB_URLS);}
    private void openUrl(String url,String msg){if(url==null||url.isEmpty()){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();return;}startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
    private int selectedLookbackDays(){int p=lookback==null?1:lookback.getSelectedItemPosition();return p==0?1:p==1?3:p==3?14:7;}
    private void setLookbackSelection(int d){lookback.setSelection(d==1?0:d==3?1:d==14?3:2);}
    private String periodLabel(int d){return d==1?"24 часа":d==3?"3 дня":d==14?"14 дней":"7 дней";}
    private int countChannels(String raw){if(raw==null||raw.trim().isEmpty())return 0;java.util.HashSet<String>u=new java.util.HashSet<>();for(String p:raw.split("[\\n,; ]+")){String c=LeadScannerV10.normalizeChannel(p);if(!c.isEmpty())u.add(c.toLowerCase(java.util.Locale.ROOT));}return u.size();}
    private void requestNotificationsIfNeeded(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);}
    private TextView label(String t){TextView v=new TextView(this);v.setText(t);v.setTextSize(15);v.setTypeface(Typeface.DEFAULT_BOLD);v.setPadding(0,dp(12),0,dp(4));return v;}
    private LinearLayout.LayoutParams fullWidth(){return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);}
    private LinearLayout.LayoutParams buttonParams(){LinearLayout.LayoutParams p=fullWidth();p.topMargin=dp(10);return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
