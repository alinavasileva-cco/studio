package com.thecase.dmqueue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_IMPORT = 1001;
    private static final int REQ_EXPORT = 1002;

    private static final int BG = Color.rgb(243,239,231);
    private static final int PANEL = Color.rgb(255,253,249);
    private static final int INK = Color.rgb(23,23,21);
    private static final int MUTED = Color.rgb(112,107,100);
    private static final int LINE = Color.rgb(221,215,205);
    private static final int ACCENT = Color.rgb(126,157,153);

    private ContactStore store;
    private List<Contact> contacts;
    private LinearLayout content;
    private CsvUtils.ImportResult pendingImport;
    private TextView importPreview;
    private CheckBox updateExisting;
    private String pendingExport = "";

    private final BroadcastReceiver pasteReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Toast.makeText(MainActivity.this,
                    "Текст вставлен в Direct. Проверьте его и отправьте вручную.",
                    Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ContactStore(this);
        contacts = store.load();
        buildShell();
        showQueue();

        IntentFilter f = new IntentFilter("com.thecase.dmqueue.PASTE_DONE");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(pasteReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(pasteReceiver, f);
        }
    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(pasteReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(20), dp(16), dp(24));

        TextView brand = tv("The Case", 34, INK, true);
        brand.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        root.addView(brand);

        TextView sub = tv("INSTAGRAM DM ASSISTANT", 11, MUTED, false);
        sub.setLetterSpacing(0.18f);
        LinearLayout.LayoutParams subLp = lp(-1, -2);
        subLp.setMargins(0, dp(4), 0, dp(14));
        root.addView(sub, subLp);

        HorizontalScrollView navScroll = new HorizontalScrollView(this);
        navScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.addView(navButton("Очередь", v -> showQueue()));
        nav.addView(navButton("Импорт", v -> showImport()));
        nav.addView(navButton("Настройки", v -> showSettings()));
        navScroll.addView(nav);
        root.addView(navScroll, lp(-1,-2));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cLp = lp(-1, 0);
        cLp.weight = 1;
        cLp.setMargins(0, dp(12), 0, 0);
        root.addView(content, cLp);

        setContentView(root);
    }

    private Button navButton(String label, View.OnClickListener l) {
        Button b = button(label, false);
        LinearLayout.LayoutParams p = lp(-2, dp(46));
        p.setMargins(0,0,dp(8),0);
        b.setLayoutParams(p);
        b.setOnClickListener(l);
        return b;
    }

    private void clearContent() {
        content.removeAllViews();
    }

    private void showQueue() {
        contacts = store.load();
        clearContent();

        LinearLayout stats = card();
        stats.addView(title("Очередь"));
        int sent=0, skipped=0, fresh=0;
        for (Contact c: contacts) {
            if ("sent".equals(c.status)) sent++;
            else if ("skipped".equals(c.status)) skipped++;
            else fresh++;
        }
        TextView s = tv(contacts.size()+" всего   •   "+fresh+" новых   •   "+sent+" отправлено   •   "+skipped+" пропущено",
                14, MUTED, false);
        stats.addView(s);
        LinearLayout actions = row();
        Button imp = button("Импортировать", true);
        imp.setOnClickListener(v -> showImport());
        actions.addView(imp, weighted());
        Button exp = button("Экспорт CSV", false);
        exp.setOnClickListener(v -> exportCsv());
        actions.addView(exp, weighted());
        stats.addView(actions);
        content.addView(stats, cardLp());

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        if (contacts.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(title("Пока пусто"));
            empty.addView(tv("Загрузите CSV, вставьте таблицу или добавьте демо-записи для проверки.", 15, MUTED, false));
            Button demo = button("Добавить 3 демо-записи", false);
            demo.setOnClickListener(v -> { addDemo(); showQueue(); });
            empty.addView(demo);
            list.addView(empty, cardLp());
        } else {
            for (Contact c: contacts) list.addView(contactCard(c), cardLp());
        }
        scroll.addView(list);
        content.addView(scroll, lp(-1,0,1f));
    }

    private View contactCard(Contact c) {
        LinearLayout card = card();
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showDetail(c.id));

        LinearLayout top = row();
        TextView h = tv("@"+c.instagram, 18, INK, true);
        top.addView(h, weighted());
        TextView st = tv(statusLabel(c.status), 12, statusColor(c.status), true);
        st.setGravity(Gravity.RIGHT);
        top.addView(st, lp(-2,-2));
        card.addView(top);

        if (c.brand != null && !c.brand.isEmpty()) {
            TextView b = tv(c.brand, 14, MUTED, false);
            card.addView(b);
        }

        String preview = c.message == null ? "" : c.message.replace("\n"," ").trim();
        if (preview.length() > 150) preview = preview.substring(0,150)+"…";
        TextView p = tv(preview, 14, INK, false);
        LinearLayout.LayoutParams pp = lp(-1,-2);
        pp.setMargins(0,dp(8),0,0);
        card.addView(p, pp);
        return card;
    }

    private void showDetail(String id) {
        contacts = store.load();
        Contact c = store.findById(contacts, id);
        if (c == null) { showQueue(); return; }
        clearContent();

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout card = card();
        card.addView(title("Контакт"));
        card.addView(label("Instagram"));
        EditText handle = edit("@"+c.instagram, false);
        card.addView(handle);

        card.addView(label("Бренд"));
        EditText brand = edit(c.brand, false);
        card.addView(brand);

        card.addView(label("Письмо"));
        EditText message = edit(c.message, true);
        message.setMinLines(10);
        card.addView(message);

        card.addView(label("Заметка"));
        EditText note = edit(c.note, true);
        note.setMinLines(2);
        card.addView(note);

        TextView status = tv("Статус: "+statusLabel(c.status), 13, MUTED, true);
        LinearLayout.LayoutParams stLp = lp(-1,-2);
        stLp.setMargins(0,dp(12),0,0);
        card.addView(status, stLp);

        Button openPaste = button("Скопировать + открыть Instagram + вставить", true);
        openPaste.setOnClickListener(v -> {
            saveEdits(c, handle, brand, message, note);
            copyText(c.message);
            armPaste(c);
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this,
                        "Автовставка не включена. Текст скопирован; в Instagram вставьте его вручную. Включить помощник можно в Настройках.",
                        Toast.LENGTH_LONG).show();
            }
            openInstagram(c.instagram, true);
        });
        card.addView(openPaste);

        Button profile = button("Только открыть профиль", false);
        profile.setOnClickListener(v -> {
            saveEdits(c, handle, brand, message, note);
            copyText(c.message);
            openInstagram(c.instagram, false);
        });
        card.addView(profile);

        LinearLayout r = row();
        Button sent = button("Отправлено", false);
        sent.setOnClickListener(v -> {
            saveEdits(c,handle,brand,message,note);
            c.status="sent"; c.updatedAt=System.currentTimeMillis(); store.save(contacts); showNext(c.id);
        });
        r.addView(sent, weighted());
        Button skip = button("Пропустить", false);
        skip.setOnClickListener(v -> {
            saveEdits(c,handle,brand,message,note);
            c.status="skipped"; c.updatedAt=System.currentTimeMillis(); store.save(contacts); showNext(c.id);
        });
        r.addView(skip, weighted());
        card.addView(r);

        Button back = button("← Назад к очереди", false);
        back.setOnClickListener(v -> { saveEdits(c,handle,brand,message,note); showQueue(); });
        card.addView(back);

        box.addView(card, cardLp());

        if (!isAccessibilityEnabled()) {
            LinearLayout hint = card();
            hint.addView(title("Чтобы письмо вставлялось автоматически"));
            hint.addView(tv("Включите сервис «The Case DM Queue» в специальных возможностях Android. Он активируется только после вашего нажатия в приложении, вставляет подготовленный текст в поле Direct и никогда не нажимает «Отправить».", 14, MUTED, false));
            Button settings = button("Открыть специальные возможности", false);
            settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
            hint.addView(settings);
            box.addView(hint, cardLp());
        }

        scroll.addView(box);
        content.addView(scroll, lp(-1,0,1f));
    }

    private void saveEdits(Contact c, EditText handle, EditText brand, EditText message, EditText note) {
        c.instagram = ContactStore.normalizeHandle(handle.getText().toString());
        c.brand = brand.getText().toString().trim();
        c.message = message.getText().toString();
        c.note = note.getText().toString().trim();
        c.updatedAt = System.currentTimeMillis();
        store.save(contacts);
    }

    private void showNext(String currentId) {
        contacts = store.load();
        int idx=-1;
        for (int i=0;i<contacts.size();i++) if (contacts.get(i).id.equals(currentId)) idx=i;
        for (int offset=1;offset<=contacts.size();offset++) {
            int j = (Math.max(0,idx)+offset) % contacts.size();
            Contact n=contacts.get(j);
            if (!"sent".equals(n.status) && !"skipped".equals(n.status)) {
                showDetail(n.id); return;
            }
        }
        showQueue();
    }

    private void showImport() {
        clearContent();
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout fileCard = card();
        fileCard.addView(title("Загрузить список"));
        fileCard.addView(tv("Основной формат: CSV. Каждая строка связывает Instagram-аккаунт с конкретным уже подготовленным письмом.", 14, MUTED, false));
        TextView fmt = tv("Минимум: instagram,message\nРасширенно: instagram,brand,message,language,note", 13, INK, true);
        LinearLayout.LayoutParams fmtLp = lp(-1,-2); fmtLp.setMargins(0,dp(10),0,dp(8)); fileCard.addView(fmt,fmtLp);
        Button choose = button("Выбрать CSV на телефоне", true);
        choose.setOnClickListener(v -> chooseCsv());
        fileCard.addView(choose);
        box.addView(fileCard, cardLp());

        LinearLayout pasteCard = card();
        pasteCard.addView(title("Или вставить таблицу"));
        final EditText batch = edit("", true);
        batch.setHint("@account<TAB>готовое письмо");
        batch.setMinLines(8);
        pasteCard.addView(batch);
        LinearLayout br = row();
        Button clip = button("Вставить из буфера", false);
        clip.setOnClickListener(v -> {
            ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount()>0) {
                batch.setText(cm.getPrimaryClip().getItemAt(0).coerceToText(this));
            }
        });
        br.addView(clip, weighted());
        Button parse = button("Проверить", true);
        parse.setOnClickListener(v -> prepareImport(batch.getText().toString()));
        br.addView(parse, weighted());
        pasteCard.addView(br);
        box.addView(pasteCard, cardLp());

        LinearLayout previewCard = card();
        previewCard.addView(title("Предпросмотр"));
        importPreview = tv("Здесь появятся валидные строки и ошибки до импорта.", 13, MUTED, false);
        previewCard.addView(importPreview);
        updateExisting = new CheckBox(this);
        updateExisting.setText("Обновлять письмо, если аккаунт уже есть");
        updateExisting.setTextColor(INK);
        updateExisting.setChecked(true);
        previewCard.addView(updateExisting);
        Button confirm = button("Подтвердить импорт", true);
        confirm.setOnClickListener(v -> confirmImport());
        previewCard.addView(confirm);
        box.addView(previewCard, cardLp());

        LinearLayout manual = card();
        manual.addView(title("Добавить одну запись"));
        EditText mh=edit("",false); mh.setHint("@instagram");
        EditText mb=edit("",false); mb.setHint("Бренд — необязательно");
        EditText mm=edit("",true); mm.setHint("Готовое письмо"); mm.setMinLines(6);
        manual.addView(mh); manual.addView(mb); manual.addView(mm);
        Button add=button("Добавить в очередь",false);
        add.setOnClickListener(v -> {
            String h=ContactStore.normalizeHandle(mh.getText().toString());
            if (h.isEmpty() || mm.getText().toString().trim().isEmpty()) {
                toast("Нужны Instagram и письмо"); return;
            }
            Contact n=new Contact(); n.instagram=h; n.brand=mb.getText().toString().trim(); n.message=mm.getText().toString();
            List<Contact> one=new ArrayList<>(); one.add(n);
            store.importContacts(contacts, one, true);
            contacts=store.load(); toast("Добавлено"); showQueue();
        });
        manual.addView(add);
        box.addView(manual, cardLp());

        scroll.addView(box);
        content.addView(scroll, lp(-1,0,1f));
    }

    private void chooseCsv() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv","text/plain","application/csv","application/vnd.ms-excel"});
        startActivityForResult(i, REQ_IMPORT);
    }

    private void prepareImport(String raw) {
        pendingImport = CsvUtils.importText(raw);
        StringBuilder sb=new StringBuilder();
        sb.append("Готово к импорту: ").append(pendingImport.valid.size()).append("\n");
        sb.append("Ошибок: ").append(pendingImport.errors.size()).append("\n\n");
        int show=Math.min(5,pendingImport.valid.size());
        for(int i=0;i<show;i++) {
            Contact c=pendingImport.valid.get(i);
            sb.append("• @").append(c.instagram);
            if(!c.brand.isEmpty()) sb.append(" — ").append(c.brand);
            sb.append("\n");
        }
        if(pendingImport.valid.size()>show) sb.append("… и ещё ").append(pendingImport.valid.size()-show).append("\n");
        if(!pendingImport.errors.isEmpty()) {
            sb.append("\nОшибки:\n");
            for(int i=0;i<Math.min(8,pendingImport.errors.size());i++) sb.append("• ").append(pendingImport.errors.get(i)).append("\n");
        }
        if(importPreview!=null) importPreview.setText(sb.toString());
    }

    private void confirmImport() {
        if (pendingImport == null || pendingImport.valid.isEmpty()) {
            toast("Сначала выберите файл или проверьте вставку");
            return;
        }
        contacts=store.load();
        int changed=store.importContacts(contacts,pendingImport.valid,updateExisting==null || updateExisting.isChecked());
        contacts=store.load();
        toast("Импортировано/обновлено: "+changed);
        pendingImport=null;
        showQueue();
    }

    private void showSettings() {
        clearContent();
        ScrollView scroll=new ScrollView(this);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout ig=card();
        ig.addView(title("Instagram"));
        ig.addView(tv("Приложение не хранит пароль и не входит в Instagram само. Используется уже авторизованная сессия официального приложения Instagram на этом телефоне.",14,MUTED,false));
        Button check=button("Открыть Instagram и проверить вход",true);
        check.setOnClickListener(v->launchInstagram());
        ig.addView(check);
        box.addView(ig,cardLp());

        LinearLayout auto=card();
        auto.addView(title("Автовставка"));
        boolean enabled=isAccessibilityEnabled();
        auto.addView(tv(enabled ? "Помощник включён." : "Помощник выключен.",14,enabled?Color.rgb(49,93,69):Color.rgb(156,59,52),true));
        auto.addView(tv("Когда помощник включён, после вашего нажатия «Скопировать + открыть Instagram + вставить» он пытается открыть Direct и заполнить поле сообщения. Отправку он никогда не нажимает.",14,MUTED,false));
        Button access=button("Настроить специальные возможности",false);
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        auto.addView(access);

        CheckBox direct=new CheckBox(this);
        direct.setText("Сначала пытаться открыть Direct по ссылке ig.me");
        direct.setTextColor(INK);
        direct.setChecked(store.prefs().getBoolean("prefer_direct",true));
        direct.setOnCheckedChangeListener((b,checked)->store.prefs().edit().putBoolean("prefer_direct",checked).apply());
        auto.addView(direct);
        box.addView(auto,cardLp());

        LinearLayout testing=card();
        testing.addView(title("О тестировании"));
        testing.addView(tv("1. Импортируйте CSV.\n2. Откройте запись.\n3. Нажмите «Скопировать + открыть Instagram + вставить».\n4. Убедитесь, что открыт нужный профиль или Direct.\n5. Проверьте вставленный текст.\n6. Нажмите «Отправить» вручную.\n7. Вернитесь в The Case DM Queue и отметьте запись «Отправлено».",14,INK,false));
        Button demo=button("Загрузить 3 демо-записи",false);
        demo.setOnClickListener(v->{addDemo();toast("Демо добавлено");showQueue();});
        testing.addView(demo);
        box.addView(testing,cardLp());

        LinearLayout data=card();
        data.addView(title("Локальные данные"));
        data.addView(tv("Очередь и статусы сохраняются только в памяти этого приложения на телефоне.",14,MUTED,false));
        Button clear=button("Очистить локальную базу",false);
        clear.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("Очистить очередь?")
                .setMessage("Все аккаунты, письма и статусы будут удалены с этого телефона.")
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Очистить",(d,w)->{store.clear();contacts=new ArrayList<>();showQueue();})
                .show());
        data.addView(clear);
        box.addView(data,cardLp());

        scroll.addView(box);
        content.addView(scroll,lp(-1,0,1f));
    }

    private void addDemo() {
        contacts=store.load();
        List<Contact> demos=new ArrayList<>();
        demos.add(demo("example_brand_one","Demo Brand One","Здравствуйте! Это тестовое письмо. Оно не будет отправлено автоматически."));
        demos.add(demo("example_brand_two","Demo Brand Two","Hi! This is a test message. The app never presses Send automatically."));
        demos.add(demo("example_brand_three","Demo Brand Three","Тест: проверить открытие профиля, вставку текста и смену статуса."));
        store.importContacts(contacts,demos,false);
        contacts=store.load();
    }

    private Contact demo(String handle,String brand,String message) {
        Contact c=new Contact(); c.instagram=handle; c.brand=brand; c.message=message; c.language=message.startsWith("Hi")?"en":"ru"; c.note="DEMO"; return c;
    }

    private void exportCsv() {
        contacts=store.load();
        pendingExport=CsvUtils.exportCsv(contacts);
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/csv");
        i.putExtra(Intent.EXTRA_TITLE,"the_case_instagram_dm_status.csv");
        startActivityForResult(i,REQ_EXPORT);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null || data.getData()==null) return;
        Uri uri=data.getData();
        if(requestCode==REQ_IMPORT) {
            try {
                prepareImport(readText(uri));
            } catch(Exception e) {
                toast("Не удалось прочитать файл: "+e.getMessage());
            }
        } else if(requestCode==REQ_EXPORT) {
            try(OutputStream os=getContentResolver().openOutputStream(uri)) {
                if(os!=null) os.write(pendingExport.getBytes(StandardCharsets.UTF_8));
                toast("CSV сохранён");
            } catch(Exception e) {
                toast("Ошибка экспорта: "+e.getMessage());
            }
        }
    }

    private String readText(Uri uri) throws Exception {
        StringBuilder sb=new StringBuilder();
        try(InputStream is=getContentResolver().openInputStream(uri);
            BufferedReader br=new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
            String line;
            while((line=br.readLine())!=null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private void copyText(String text) {
        ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("The Case DM",text==null?"":text));
        Toast.makeText(this,"Письмо скопировано",Toast.LENGTH_SHORT).show();
    }

    private void armPaste(Contact c) {
        store.prefs().edit()
                .putBoolean("pending_paste",true)
                .putString("pending_message",c.message)
                .putString("pending_handle",c.instagram)
                .putString("pending_stage","open")
                .putLong("pending_at",System.currentTimeMillis())
                .apply();
    }

    private void openInstagram(String handle, boolean direct) {
        String h=ContactStore.normalizeHandle(handle);
        boolean preferDirect=store.prefs().getBoolean("prefer_direct",true);
        if(direct && preferDirect) {
            Intent dm=new Intent(Intent.ACTION_VIEW, Uri.parse("https://ig.me/m/"+Uri.encode(h)));
            dm.setPackage("com.instagram.android");
            try { startActivity(dm); return; } catch(ActivityNotFoundException ignored) {}
        }
        Intent profile=new Intent(Intent.ACTION_VIEW,Uri.parse("instagram://user?username="+Uri.encode(h)));
        profile.setPackage("com.instagram.android");
        try { startActivity(profile); return; } catch(ActivityNotFoundException ignored) {}
        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.instagram.com/"+Uri.encode(h)+"/")));
    }

    private void launchInstagram() {
        Intent i=getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if(i!=null) startActivity(i);
        else startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.instagram.com/")));
    }

    private boolean isAccessibilityEnabled() {
        String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(enabled==null) return false;
        String full=new ComponentName(this,InstagramAccessibilityService.class).flattenToString();
        return enabled.toLowerCase(Locale.ROOT).contains(full.toLowerCase(Locale.ROOT))
                || enabled.toLowerCase(Locale.ROOT).contains((getPackageName()+"/.InstagramAccessibilityService").toLowerCase(Locale.ROOT));
    }

    private String statusLabel(String s) {
        if("sent".equals(s)) return "ОТПРАВЛЕНО";
        if("skipped".equals(s)) return "ПРОПУЩЕНО";
        if("error".equals(s)) return "ОШИБКА";
        if("prepared".equals(s)) return "ГОТОВО";
        return "НОВОЕ";
    }

    private int statusColor(String s) {
        if("sent".equals(s)) return Color.rgb(49,93,69);
        if("skipped".equals(s)) return MUTED;
        if("error".equals(s)) return Color.rgb(156,59,52);
        return ACCENT;
    }

    private LinearLayout card() {
        LinearLayout x=new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable g=new GradientDrawable();
        g.setColor(PANEL); g.setCornerRadius(dp(18)); g.setStroke(dp(1),LINE);
        x.setBackground(g);
        return x;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams p=lp(-1,-2);
        p.setMargins(0,0,0,dp(12));
        return p;
    }

    private LinearLayout row() {
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams p=lp(-1,-2); p.setMargins(0,dp(10),0,0); r.setLayoutParams(p);
        return r;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams p=lp(0,-2,1f);
        p.setMargins(0,0,dp(6),0);
        return p;
    }

    private TextView title(String s) {
        TextView t=tv(s,23,INK,true);
        t.setTypeface(Typeface.create("serif",Typeface.NORMAL));
        LinearLayout.LayoutParams p=lp(-1,-2); p.setMargins(0,0,0,dp(10)); t.setLayoutParams(p);
        return t;
    }

    private TextView label(String s) {
        TextView t=tv(s,12,MUTED,true);
        LinearLayout.LayoutParams p=lp(-1,-2); p.setMargins(0,dp(10),0,dp(5)); t.setLayoutParams(p);
        return t;
    }

    private TextView tv(String s,float size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL)); t.setLineSpacing(0,1.08f);
        return t;
    }

    private EditText edit(String s,boolean multiline) {
        EditText e=new EditText(this);
        e.setText(s==null?"":s); e.setTextColor(INK); e.setTextSize(15); e.setHintTextColor(Color.rgb(150,145,138));
        e.setPadding(dp(12),dp(11),dp(12),dp(11));
        if(multiline) { e.setSingleLine(false); e.setGravity(Gravity.TOP); }
        else e.setSingleLine(true);
        GradientDrawable g=new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(12)); g.setStroke(dp(1),LINE); e.setBackground(g);
        LinearLayout.LayoutParams p=lp(-1,-2); p.setMargins(0,0,0,dp(8)); e.setLayoutParams(p);
        return e;
    }

    private Button button(String s,boolean primary) {
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(primary?Color.WHITE:INK);
        GradientDrawable g=new GradientDrawable(); g.setColor(primary?INK:Color.WHITE); g.setCornerRadius(dp(13)); g.setStroke(dp(1),primary?INK:LINE); b.setBackground(g);
        LinearLayout.LayoutParams p=lp(-1,dp(50)); p.setMargins(0,dp(10),0,0); b.setLayoutParams(p);
        return b;
    }

    private LinearLayout.LayoutParams lp(int w,int h) { return new LinearLayout.LayoutParams(w,h); }
    private LinearLayout.LayoutParams lp(int w,int h,float weight) { return new LinearLayout.LayoutParams(w,h,weight); }
    private int dp(int v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
}
