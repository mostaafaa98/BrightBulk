package com.brightbulk.app

import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var db: LocalDb
    private lateinit var body: LinearLayout
    private var selectedChannel = "WhatsApp Business"
    private var campaignContacts = mutableListOf<Contact>()
    private var campaignIndex = 0
    private var campaignMessage = ""
    private var pendingResume = false

    private val bg = Color.rgb(10,11,14)
    private val card = Color.rgb(23,25,30)
    private val card2 = Color.rgb(31,34,40)
    private val fg = Color.rgb(245,247,250)
    private val muted = Color.rgb(155,163,175)
    private val green = Color.rgb(60,190,125)

    private fun dp(v:Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        db = LocalDb(this)
        shell()
        home()
    }

    private fun shell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(Color.rgb(15,17,21))
        }
        top.addView(tv("B",22f,true), LinearLayout.LayoutParams(dp(46),dp(46)))
        val brand = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),0,0,0) }
        brand.addView(tv("BRIGHT BULK",18f,true))
        brand.addView(tv("WhatsApp Campaign Manager",11f).apply { setTextColor(green) })
        top.addView(brand, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(top, LinearLayout.LayoutParams(-1,dp(70)))

        val scroll = ScrollView(this)
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16),dp(16),dp(16),dp(20))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        val nav = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.rgb(15,17,21)) }
        listOf("الرئيسية" to ::home, "العملاء" to ::contacts, "الحملات" to ::campaigns, "الإعدادات" to ::settings)
            .forEach { (name, action) ->
                nav.addView(tv(name,10f,true).apply {
                    gravity=Gravity.CENTER
                    setPadding(dp(4),dp(15),dp(4),dp(15))
                    setOnClickListener { action() }
                }, LinearLayout.LayoutParams(0,dp(68),1f))
            }
        root.addView(nav)
        setContentView(root)
    }

    private fun tv(s:String,size:Float=14f,bold:Boolean=false)=TextView(this).apply{
        text=s; textSize=size; setTextColor(fg)
        setPadding(dp(4),dp(4),dp(4),dp(4))
        if(bold) setTypeface(null,1)
    }

    private fun btn(s:String, primary:Boolean=false, click:()->Unit)=tv(s,14f,true).apply{
        gravity=Gravity.CENTER
        setPadding(dp(12),dp(12),dp(12),dp(12))
        setBackgroundColor(if(primary) Color.rgb(34,105,76) else card2)
        setOnClickListener{click()}
    }

    private fun page(title:String, sub:String="") {
        body.removeAllViews()
        body.addView(tv(title,26f,true))
        if(sub.isNotBlank()) body.addView(tv(sub,13f).apply{setTextColor(muted)})
    }

    private fun section(s:String) {
        body.addView(tv(s,16f,true).apply{setPadding(0,dp(18),0,dp(8))})
    }

    private fun addButton(s:String, primary:Boolean=false, action:()->Unit) {
        body.addView(btn(s,primary,action), LinearLayout.LayoutParams(-1,dp(52)).apply{
            setMargins(0,0,0,dp(8))
        })
    }

    private fun home() {
        page("الرئيسية","إدارة عملاء وحملات WhatsApp من الموبايل.")
        section("الحالة")
        body.addView(tv("WhatsApp يعمل من خلال تطبيق WhatsApp نفسه — بدون Cloud API وبدون Accessibility.",13f).apply{setTextColor(green)})
        section("إجراءات سريعة")
        addButton("＋ إضافة عميل",true){addContact()}
        addButton("⇩ استيراد CSV / TXT"){pickFile()}
        addButton("＋ إنشاء حملة WhatsApp"){newCampaign()}
        section("الإحصائيات")
        body.addView(tv("العملاء: ${db.countContacts()}\nالحملات: ${db.countCampaigns()}\nتم فتح محادثات: ${db.countOpened()}",14f))
    }

    private fun contacts() {
        page("العملاء","أضف العملاء أو استوردهم من CSV/TXT.")
        addButton("＋ إضافة عميل",true){addContact()}
        addButton("⇩ استيراد CSV / TXT"){pickFile()}
        section("قائمة العملاء")
        val list=db.contacts()
        if(list.isEmpty()) body.addView(tv("لا يوجد عملاء بعد.",13f).apply{setTextColor(muted)})
        list.forEach { c ->
            val box=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL
                setPadding(dp(12),dp(10),dp(12),dp(10))
                setBackgroundColor(card)
            }
            box.addView(tv(c.name,15f,true))
            box.addView(tv(c.phone,12f).apply{setTextColor(muted)})
            body.addView(box,LinearLayout.LayoutParams(-1,dp(74)).apply{setMargins(0,0,0,dp(7))})
        }
    }

    private fun campaigns() {
        page("الحملات","الحملات هنا يدوية بمساعدة WhatsApp: أنت تضغط إرسال داخل WhatsApp.")
        addButton("＋ حملة WhatsApp جديدة",true){newCampaign()}
        section("الحملات المحفوظة")
        db.campaigns().forEach { c ->
            val box=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL; setPadding(dp(12),dp(10),dp(12),dp(10)); setBackgroundColor(card)
            }
            box.addView(tv(c.name,15f,true))
            box.addView(tv("${c.total} عميل • ${c.opened} تم فتحهم • ${c.sent} تم تسجيل إرسالهم",11f).apply{setTextColor(muted)})
            box.addView(btn("▶ بدء الحملة"){startCampaign(c)})
            body.addView(box,LinearLayout.LayoutParams(-1,dp(125)).apply{setMargins(0,0,0,dp(8))})
        }
    }

    private fun settings() {
        page("الإعدادات","إعداد WhatsApp المستخدم.")
        section("تطبيق WhatsApp")
        val sp=Spinner(this)
        sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,
            arrayOf("WhatsApp Business","WhatsApp العادي"))
        sp.setSelection(if(selectedChannel=="WhatsApp العادي")1 else 0)
        sp.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p:android.widget.AdapterView<*>?) {}
            override fun onItemSelected(p:android.widget.AdapterView<*>?,v:View?,pos:Int,id:Long){
                selectedChannel=if(pos==1)"WhatsApp العادي" else "WhatsApp Business"
            }
        }
        body.addView(sp)
        section("تنبيه")
        body.addView(tv("لن يتم الضغط على زر إرسال تلقائيًا داخل WhatsApp. بعد فتح المحادثة والرسالة الجاهزة، أنت تضغط إرسال.",13f).apply{setTextColor(muted)})
    }

    private fun newCampaign() {
        val name=EditText(this).apply{hint="اسم الحملة";setTextColor(fg);setHintTextColor(muted)}
        val msg=EditText(this).apply{
            hint="الرسالة — يمكنك استخدام {{name}}"
            minLines=5; gravity=Gravity.TOP; setTextColor(fg); setHintTextColor(muted)
        }
        val sp=Spinner(this)
        sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("WhatsApp Business","WhatsApp العادي"))
        val l=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setPadding(dp(18),0,dp(18),0)
            addView(name);addView(sp);addView(msg)
        }
        AlertDialog.Builder(this).setTitle("حملة جديدة").setView(l)
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("حفظ وبدء"){_,_->
                val ch=sp.selectedItem.toString()
                val contacts=db.contacts()
                if(contacts.isEmpty()){toast("أضف عملاء أولًا");return@setPositiveButton}
                val id=db.addCampaign(name.text.toString(),ch,msg.text.toString(),contacts.size)
                startCampaign(db.campaign(id)!!)
            }.show()
    }

    private fun startCampaign(c:Campaign) {
        campaignContacts=db.contacts().toMutableList()
        if(campaignContacts.isEmpty()){toast("لا يوجد عملاء");return}
        campaignIndex=0
        campaignMessage=c.message
        selectedChannel=c.channel
        db.markCampaignStarted(c.id)
        openCurrent(c.id)
    }

    private fun openCurrent(campaignId:String) {
        if(campaignIndex>=campaignContacts.size){
            db.completeCampaign(campaignId)
            toast("انتهت الحملة")
            campaigns()
            return
        }
        val c=campaignContacts[campaignIndex]
        val text=campaignMessage.replace("{{name}}",c.name).replace("{{phone}}",c.phone)
        val number=c.phone.filter{it.isDigit()}
        if(number.isBlank()){advance(campaignId);return}
        val uri=Uri.parse("https://wa.me/$number?text="+Uri.encode(text))
        val intent=Intent(Intent.ACTION_VIEW,uri)
        val pkg=if(selectedChannel=="WhatsApp Business")"com.whatsapp.w4b" else "com.whatsapp"
        intent.setPackage(pkg)
        try{
            pendingResume=true
            db.markOpened(campaignId,c.id)
            startActivity(intent)
        }catch(e:Exception){
            pendingResume=false
            val chooser=Intent(Intent.ACTION_VIEW,uri)
            try{startActivity(chooser)}catch(_ :Exception){toast("WhatsApp غير مثبت")}
        }
    }

    override fun onResume(){
        super.onResume()
        if(pendingResume){
            pendingResume=false
            showNextDialog()
        }
    }

    private fun showNextDialog(){
        if(campaignIndex>=campaignContacts.size)return
        val c=campaignContacts[campaignIndex]
        AlertDialog.Builder(this)
            .setTitle("تم فتح المحادثة")
            .setMessage("${c.name}\n\nاضغط إرسال داخل WhatsApp، ثم ارجع واضغط «التالي».")
            .setNegativeButton("تخطي"){_,_->advanceFromDialog(false)}
            .setPositiveButton("التالي"){_,_->advanceFromDialog(true)}
            .setCancelable(false).show()
    }

    private fun advanceFromDialog(sent:Boolean){
        if(campaignIndex<campaignContacts.size){
            val c=campaignContacts[campaignIndex]
            db.markResult(c.id,sent)
        }
        campaignIndex++
        val campaign=db.runningCampaign()
        if(campaign!=null)openCurrent(campaign.id) else campaigns()
    }

    private fun advance(campaignId:String){
        campaignIndex++
        openCurrent(campaignId)
    }

    private fun addContact(){
        val n=EditText(this).apply{hint="الاسم"}
        val p=EditText(this).apply{hint="رقم الهاتف بصيغة دولية"}
        val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),0,dp(18),0);addView(n);addView(p)}
        AlertDialog.Builder(this).setTitle("إضافة عميل").setView(l)
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("حفظ"){_,_->db.addContact(n.text.toString(),p.text.toString());contacts()}.show()
    }

    private fun pickFile(){
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            type="text/*";addCategory(Intent.CATEGORY_OPENABLE)
        },90)
    }

    override fun onActivityResult(r:Int,c:Int,data:Intent?){
        super.onActivityResult(r,c,data)
        if(r==90&&c==RESULT_OK&&data?.data!=null)importCsv(data.data!!)
    }

    private fun importCsv(uri:Uri){
        Thread{
            var count=0
            contentResolver.openInputStream(uri)?.use{input->
                BufferedReader(InputStreamReader(input)).use{br->
                    var line=br.readLine()
                    while(line!=null){
                        val parts=parseLine(line!!)
                        if(parts.size>=2){
                            val a=parts[0].trim();val b=parts[1].trim()
                            val phone=if(a.any{it.isDigit()})a else b
                            val name=if(a.any{it.isDigit()})b else a
                            if(phone.filter{it.isDigit()}.length>=7){db.addContact(name,phone);count++}
                        }
                        line=br.readLine()
                    }
                }
            }
            runOnUiThread{toast("تم استيراد $count عميل");contacts()}
        }.start()
    }

    private fun parseLine(s:String):List<String>{
        val sep=when{
            s.contains(';')->';'
            s.contains('\t')->'\t'
            else->','
        }
        return s.split(sep)
    }

    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()

    data class Contact(val id:String,val name:String,val phone:String)
    data class Campaign(val id:String,val name:String,val channel:String,val message:String,val status:String,val total:Int,val opened:Int,val sent:Int)

    class LocalDb(ctx:Context):SQLiteOpenHelper(ctx,"brightbulk_final.db",null,1){
        override fun onCreate(d:android.database.sqlite.SQLiteDatabase){
            d.execSQL("CREATE TABLE contacts(id TEXT PRIMARY KEY,name TEXT NOT NULL,phone TEXT NOT NULL UNIQUE)")
            d.execSQL("CREATE TABLE campaigns(id TEXT PRIMARY KEY,name TEXT,channel TEXT,message TEXT,status TEXT,total INTEGER,opened INTEGER,sent INTEGER)")
        }
        override fun onUpgrade(d:android.database.sqlite.SQLiteDatabase,o:Int,n:Int){}
        fun addContact(n:String,p:String){
            val phone=p.trim()
            if(phone.filter{it.isDigit()}.length<7)return
            writableDatabase.execSQL("INSERT OR IGNORE INTO contacts VALUES(?,?,?)",
                arrayOf(UUID.randomUUID().toString(),n.ifBlank{"عميل"},phone))
        }
        fun contacts():List<Contact>{
            val a=mutableListOf<Contact>()
            readableDatabase.rawQuery("SELECT id,name,phone FROM contacts ORDER BY rowid DESC",null).use{
                while(it.moveToNext())a.add(Contact(it.getString(0),it.getString(1),it.getString(2)))
            }
            return a
        }
        fun countContacts()=scalar("SELECT COUNT(*) FROM contacts")
        fun countCampaigns()=scalar("SELECT COUNT(*) FROM campaigns")
        fun countOpened()=scalar("SELECT COALESCE(SUM(opened),0) FROM campaigns")
        private fun scalar(q:String):Int=readableDatabase.rawQuery(q,null).use{it.moveToFirst();it.getInt(0)}
        fun addCampaign(n:String,ch:String,m:String,total:Int):String{
            val id=UUID.randomUUID().toString()
            writableDatabase.execSQL("INSERT INTO campaigns VALUES(?,?,?,?,?,?,?,?,?)",
                arrayOf(id,n.ifBlank{"حملة WhatsApp"},ch,m,"running",total,0,0))
            return id
        }
        fun campaign(id:String):Campaign?=one("SELECT * FROM campaigns WHERE id=?",arrayOf(id))
        fun runningCampaign():Campaign?=one("SELECT * FROM campaigns WHERE status='running' ORDER BY rowid DESC LIMIT 1",null)
        private fun one(q:String,args:Array<String>?):Campaign?{
            readableDatabase.rawQuery(q,args).use{
                if(!it.moveToFirst())return null
                return Campaign(it.getString(0),it.getString(1),it.getString(2),it.getString(3),it.getString(4),it.getInt(5),it.getInt(6),it.getInt(7))
            }
        }
        fun campaigns():List<Campaign>{
            val a=mutableListOf<Campaign>()
            readableDatabase.rawQuery("SELECT * FROM campaigns ORDER BY rowid DESC",null).use{
                while(it.moveToNext())a.add(Campaign(it.getString(0),it.getString(1),it.getString(2),it.getString(3),it.getInt(5),it.getInt(6),it.getInt(7)))
            }
            return a
        }
        fun markCampaignStarted(id:String){writableDatabase.execSQL("UPDATE campaigns SET status='running' WHERE id=?",arrayOf(id))}
        fun markOpened(cid:String,contactId:String){writableDatabase.execSQL("UPDATE campaigns SET opened=opened+1 WHERE id=?",arrayOf(cid))}
        fun markResult(contactId:String,sent:Boolean){
            val c=runningCampaign() ?: return
            if(sent)writableDatabase.execSQL("UPDATE campaigns SET sent=sent+1 WHERE id=?",arrayOf(c.id))
        }
        fun completeCampaign(id:String){writableDatabase.execSQL("UPDATE campaigns SET status='completed' WHERE id=?",arrayOf(id))}
    }
}
