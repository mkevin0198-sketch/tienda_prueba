package com.kevin.documentosvehiculos;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {

    private static final int REQ_PICK_DOCS = 5001;
    private static final String PREFS = "vehiculos_prefs";
    private static final String KEY_DATA = "data";
    private final ArrayList<Vehicle> vehicles = new ArrayList<>();
    private String pendingVehicleId = null;
    private LinearLayout root;
    private boolean dark = false;
    private String screen = "home";
    private String currentVehicleId = null;

    static class DocumentItem {
        String id, name, uri, mime;
        DocumentItem(String id, String name, String uri, String mime) {
            this.id=id; this.name=name; this.uri=uri; this.mime=mime;
        }
    }

    static class Vehicle {
        String id, name, icon;
        ArrayList<DocumentItem> docs = new ArrayList<>();
        Vehicle(String id, String name, String icon) {
            this.id=id; this.name=name; this.icon=icon;
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        loadData();
        renderHome();
    }

    private int bg(){ return dark ? Color.rgb(15,17,21) : Color.rgb(238,241,244); }
    private int card(){ return dark ? Color.rgb(23,26,32) : Color.WHITE; }
    private int text(){ return dark ? Color.rgb(243,244,246) : Color.rgb(23,32,42); }
    private int muted(){ return dark ? Color.rgb(170,178,191) : Color.rgb(102,112,133); }
    private int soft(){ return dark ? Color.rgb(38,43,51) : Color.rgb(238,243,248); }
    private int accent(){ return dark ? Color.rgb(111,177,255) : Color.rgb(11,99,206); }

    private TextView tv(String s, float sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(text());
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout box() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18),dp(16),dp(18),dp(16));
        l.setBackgroundColor(card());
        return l;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(text());
        b.setAllCaps(false);
        b.setBackgroundColor(soft());
        return b;
    }

    private void baseScreen(String title, String subtitle, boolean back, View.OnClickListener backAction) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg());

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10),dp(8),dp(10),dp(8));
        bar.setBackgroundColor(card());

        if (back) {
            Button b = button("←");
            b.setTextSize(22);
            b.setOnClickListener(backAction);
            bar.addView(b, new LinearLayout.LayoutParams(dp(52),dp(52)));
        }

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView t = tv(title,18,true);
        TextView st = tv(subtitle,12,false); st.setTextColor(muted());
        names.addView(t); names.addView(st);
        bar.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1));

        Button manage = button("⚙");
        manage.setTextSize(20);
        manage.setOnClickListener(v -> renderManager());
        bar.addView(manage, new LinearLayout.LayoutParams(dp(52),dp(52)));

        Button theme = button(dark ? "☀" : "☾");
        theme.setTextSize(20);
        theme.setOnClickListener(v -> { dark=!dark; rerender(); });
        bar.addView(theme, new LinearLayout.LayoutParams(dp(52),dp(52)));

        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(68)));
        setContentView(root);
    }

    private ScrollView contentScroll() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        return sv;
    }

    private LinearLayout contentColumn(ScrollView sv) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14),dp(18),dp(14),dp(28));
        sv.addView(c, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        return c;
    }

    private void renderHome() {
        screen="home"; currentVehicleId=null;
        baseScreen("Documentos de vehículos","Selecciona un vehículo",false,null);
        ScrollView sv=contentScroll();
        LinearLayout c=contentColumn(sv);

        TextView h=tv("Mis documentos",32,true);
        h.setGravity(Gravity.CENTER);
        c.addView(h);
        TextView p=tv("Elige el vehículo que quieras revisar.",16,false);
        p.setTextColor(muted()); p.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2); pp.bottomMargin=dp(22);
        c.addView(p,pp);

        for (Vehicle vehicle: vehicles) c.addView(vehicleCard(vehicle), cardLp());

        LinearLayout add=box();
        add.addView(tv("＋ Agregar vehículo",20,true));
        TextView d=tv("Crea un auto, moto u otro vehículo y luego carga sus documentos.",14,false);
        d.setTextColor(muted()); add.addView(d);
        add.setOnClickListener(v -> dialogAddVehicle());
        c.addView(add,cardLp());
    }

    private View vehicleCard(Vehicle v) {
        LinearLayout cardv=box();
        TextView icon=tv(v.icon,38,false); cardv.addView(icon);
        cardv.addView(tv(v.name,22,true));
        TextView meta=tv(v.docs.size()+" documento(s)",14,false); meta.setTextColor(muted()); cardv.addView(meta);
        TextView open=tv("Ver documentos →",15,true); open.setTextColor(accent());
        LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,-2); op.topMargin=dp(14);
        cardv.addView(open,op);
        cardv.setOnClickListener(x -> renderVehicle(v.id));
        return cardv;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=dp(12); return lp;
    }

    private void renderVehicle(String id) {
        Vehicle v=findVehicle(id); if(v==null){renderHome(); return;}
        screen="vehicle"; currentVehicleId=id;
        baseScreen(v.name,v.docs.size()+" documento(s)",true,x->renderHome());
        ScrollView sv=contentScroll();
        LinearLayout c=contentColumn(sv);

        if(v.docs.isEmpty()) {
            TextView e=tv("Este vehículo todavía no tiene documentos.",16,false);
            e.setTextColor(muted()); e.setGravity(Gravity.CENTER); e.setPadding(0,dp(40),0,dp(30));
            c.addView(e);
        } else {
            for(DocumentItem d:v.docs) c.addView(documentCard(v,d),cardLp());
        }
        Button add=button("＋ Agregar documentos");
        add.setOnClickListener(x->pickDocuments(v.id));
        c.addView(add,new LinearLayout.LayoutParams(-1,dp(56)));
    }

    private View documentCard(Vehicle v, DocumentItem d) {
        LinearLayout b=box();
        b.addView(tv("📄  "+d.name,17,true));
        TextView m=tv(d.mime==null?"":d.mime,12,false);m.setTextColor(muted());b.addView(m);

        LinearLayout acts=new LinearLayout(this); acts.setPadding(0,dp(10),0,0);
        Button open=button("Abrir");
        open.setOnClickListener(x->openDocument(d));
        acts.addView(open,new LinearLayout.LayoutParams(0,dp(48),1));
        Button rename=button("Renombrar");
        rename.setOnClickListener(x->dialogRenameDoc(v,d));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(48),1);rp.leftMargin=dp(6);
        acts.addView(rename,rp);
        Button del=button("Eliminar");
        del.setOnClickListener(x->confirmDeleteDoc(v,d));
        LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(0,dp(48),1);dpv.leftMargin=dp(6);
        acts.addView(del,dpv);
        b.addView(acts);
        return b;
    }

    private void renderManager() {
        screen="manager";
        baseScreen("Gestionar vehículos","Agregar, editar o eliminar",true,x->renderHome());
        ScrollView sv=contentScroll(); LinearLayout c=contentColumn(sv);

        Button add=button("＋ Agregar nuevo vehículo");
        add.setOnClickListener(x->dialogAddVehicle());
        LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,dp(56));alp.bottomMargin=dp(14);c.addView(add,alp);

        for(Vehicle v:vehicles){
            LinearLayout b=box();
            b.addView(tv(v.icon+"  "+v.name,19,true));
            TextView n=tv(v.docs.size()+" documento(s)",13,false);n.setTextColor(muted());b.addView(n);
            LinearLayout acts=new LinearLayout(this);acts.setPadding(0,dp(10),0,0);
            Button edit=button("Editar");
            edit.setOnClickListener(x->dialogEditVehicle(v));
            acts.addView(edit,new LinearLayout.LayoutParams(0,dp(48),1));
            Button docs=button("Docs");
            docs.setOnClickListener(x->renderVehicle(v.id));
            LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(0,dp(48),1);dlp.leftMargin=dp(6);acts.addView(docs,dlp);
            Button del=button("Eliminar");
            del.setOnClickListener(x->confirmDeleteVehicle(v));
            LinearLayout.LayoutParams xlp=new LinearLayout.LayoutParams(0,dp(48),1);xlp.leftMargin=dp(6);acts.addView(del,xlp);
            b.addView(acts);
            c.addView(b,cardLp());
        }
    }

    private void dialogAddVehicle() {
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),0,dp(18),0);
        EditText name=new EditText(this);name.setHint("Nombre, ej. Suzuki Baleno");
        EditText icon=new EditText(this);icon.setHint("Ícono, ej. 🚗");
        form.addView(name);form.addView(icon);
        new AlertDialog.Builder(this).setTitle("Nuevo vehículo").setView(form)
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Agregar",(d,w)->{
                    String n=name.getText().toString().trim();
                    if(n.isEmpty()) return;
                    String i=icon.getText().toString().trim(); if(i.isEmpty())i="🚗";
                    Vehicle v=new Vehicle(UUID.randomUUID().toString(),n,i);
                    vehicles.add(v);saveData();renderManager();
                }).show();
    }

    private void dialogEditVehicle(Vehicle v) {
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),0,dp(18),0);
        EditText name=new EditText(this);name.setText(v.name);
        EditText icon=new EditText(this);icon.setText(v.icon);
        form.addView(name);form.addView(icon);
        new AlertDialog.Builder(this).setTitle("Editar vehículo").setView(form)
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Guardar",(d,w)->{
                    String n=name.getText().toString().trim(); if(!n.isEmpty())v.name=n;
                    String i=icon.getText().toString().trim(); if(!i.isEmpty())v.icon=i;
                    saveData();renderManager();
                }).show();
    }

    private void dialogRenameDoc(Vehicle v, DocumentItem doc) {
        EditText e=new EditText(this);e.setText(doc.name);e.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Renombrar documento").setView(e)
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Guardar",(d,w)->{
                    String n=e.getText().toString().trim(); if(!n.isEmpty())doc.name=n;
                    saveData();renderVehicle(v.id);
                }).show();
    }

    private void confirmDeleteVehicle(Vehicle v) {
        new AlertDialog.Builder(this).setTitle("Eliminar vehículo")
                .setMessage("¿Eliminar "+v.name+" y su lista de documentos?")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Eliminar",(d,w)->{
                    vehicles.remove(v);saveData();renderManager();
                }).show();
    }

    private void confirmDeleteDoc(Vehicle v, DocumentItem doc) {
        new AlertDialog.Builder(this).setTitle("Eliminar documento")
                .setMessage("¿Eliminar "+doc.name+"?")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Eliminar",(d,w)->{
                    v.docs.remove(doc);saveData();renderVehicle(v.id);
                }).show();
    }

    private void pickDocuments(String vehicleId) {
        pendingVehicleId=vehicleId;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/pdf","image/*"});
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i,REQ_PICK_DOCS);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=REQ_PICK_DOCS || resultCode!=RESULT_OK || data==null || pendingVehicleId==null)return;
        Vehicle v=findVehicle(pendingVehicleId); if(v==null)return;

        if(data.getClipData()!=null) {
            for(int x=0;x<data.getClipData().getItemCount();x++) addUri(v,data.getClipData().getItemAt(x).getUri());
        } else if(data.getData()!=null) addUri(v,data.getData());

        saveData(); renderVehicle(v.id);
    }

    private void addUri(Vehicle v, Uri uri) {
        try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(Exception ignored){}
        String name=getDisplayName(uri);
        String mime=getContentResolver().getType(uri);
        v.docs.add(new DocumentItem(UUID.randomUUID().toString(),name,uri.toString(),mime==null?"":mime));
    }

    private String getDisplayName(Uri uri) {
        String name="Documento";
        Cursor c=null;
        try {
            c=getContentResolver().query(uri,null,null,null,null);
            if(c!=null && c.moveToFirst()) {
                int ix=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if(ix>=0) name=c.getString(ix);
            }
        } catch(Exception ignored) {}
        finally { if(c!=null)c.close(); }
        return name;
    }

    private void openDocument(DocumentItem d) {
        try {
            Intent i=new Intent(Intent.ACTION_VIEW);
            Uri u=Uri.parse(d.uri);
            i.setDataAndType(u,(d.mime==null||d.mime.isEmpty())?"*/*":d.mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch(Exception e) {
            Toast.makeText(this,"No hay una aplicación compatible para abrir este documento.",Toast.LENGTH_LONG).show();
        }
    }

    private Vehicle findVehicle(String id) {
        for(Vehicle v:vehicles) if(v.id.equals(id)) return v;
        return null;
    }

    private void saveData() {
        try {
            JSONArray arr=new JSONArray();
            for(Vehicle v:vehicles){
                JSONObject o=new JSONObject();
                o.put("id",v.id);o.put("name",v.name);o.put("icon",v.icon);
                JSONArray docs=new JSONArray();
                for(DocumentItem d:v.docs){
                    JSONObject x=new JSONObject();
                    x.put("id",d.id);x.put("name",d.name);x.put("uri",d.uri);x.put("mime",d.mime);
                    docs.put(x);
                }
                o.put("docs",docs);arr.put(o);
            }
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_DATA,arr.toString()).apply();
        } catch(Exception ignored){}
    }

    private void loadData() {
        String s=getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_DATA,null);
        if(s==null) {
            vehicles.add(new Vehicle("suzuki","Suzuki Baleno","🚗"));
            vehicles.add(new Vehicle("dominar","Bajaj Dominar 250","🏍️"));
            saveData(); return;
        }
        try {
            JSONArray arr=new JSONArray(s);
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i);
                Vehicle v=new Vehicle(o.getString("id"),o.getString("name"),o.optString("icon","🚗"));
                JSONArray docs=o.optJSONArray("docs");
                if(docs!=null) for(int j=0;j<docs.length();j++){
                    JSONObject x=docs.getJSONObject(j);
                    v.docs.add(new DocumentItem(x.getString("id"),x.getString("name"),x.getString("uri"),x.optString("mime","")));
                }
                vehicles.add(v);
            }
        } catch(Exception e) {
            vehicles.clear();
            vehicles.add(new Vehicle("suzuki","Suzuki Baleno","🚗"));
            vehicles.add(new Vehicle("dominar","Bajaj Dominar 250","🏍️"));
        }
    }

    private void rerender() {
        if("vehicle".equals(screen) && currentVehicleId!=null) renderVehicle(currentVehicleId);
        else if("manager".equals(screen)) renderManager();
        else renderHome();
    }

    @Override
    public void onBackPressed() {
        if(!"home".equals(screen)){renderHome();} else super.onBackPressed();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
