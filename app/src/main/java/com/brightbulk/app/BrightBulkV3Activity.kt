package com.brightbulk.app

import android.app.*
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class BrightBulkV3Activity : Activity() {
    private val bg=Color.rgb(9,10,12); private val card=Color.rgb(20,22,26)
    private val card2=Color.rgb(29,32,38); private val white=Color.rgb(244,246,249)
    private val gray=Color.rgb(151,158,170); private val green=Color.rgb(65,190,125)
    private lateinit var body:LinearLayout; private lateinit var nav:LinearLayout
    private var active="الرئيسية"
    private fun dp(n:Int)= (n*resources.displayMetrics.density).toInt()
    private fun shape(c:Int,r:Int=16)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;cornerRadius=dp(r).toFloat();setColor(c)}
    private fun t(s:String,z:Float=14f,b:Boolean=false)=TextView(this).apply{text=s;textSize=z;setTextColor(white);setPadding(dp(3),dp(4),dp(3),dp(4));if(b)typeface=Typeface.DEFAULT_BOLD}
    private fun m(s:String,z:Float=12f)=t(s,z).apply{setTextColor(gray)}
    private fun b(s:String,primary:Boolean=false)=t(s,13.5f,primary).apply{gravity=Gravity.CENTER;background=shape(if(primary)Color.rgb(43,91,70) else card2);minHeight=dp(48);setPadding(dp(14),dp(10),dp(14),dp(10))}
    override fun onCreate(x:Bundle?){super.onCreate(x);shell();dashboard()}
    private fun shell(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);layoutDirection=View.LAYOUT_DIRECTION_RTL}
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));setBackgroundColor(Color.rgb(15,17,20))}
        top.addView(t("B",22f,true).apply{gravity=Gravity.CENTER;background=shape(card2);setPadding(dp(12),0,dp(12),0)},LinearLayout.LayoutParams(dp(50),dp(50)))
        val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,0,0)}
        brand.addView(t("BRIGHT BULK",19f,true));brand.addView(m("Omnichannel • V3",11f));top.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
        top.addView(m("● LIVE",11f,true).apply{setTextColor(green)});root.addView(top,LinearLayout.LayoutParams(-1,dp(74)))
        val sc=ScrollView(this);body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(18),dp(16),dp(22));layoutDirection=View.LAYOUT_DIRECTION_RTL};sc.addView(body);root.addView(sc,LinearLayout.LayoutParams(-1,0,1f))
        nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setBackgroundColor(Color.rgb(15,17,20));setPadding(dp(6),dp(5),dp(6),dp(7))};root.addView(nav,LinearLayout.LayoutParams(-1,dp(74)));setContentView(root);drawNav()
    }
    private fun drawNav(){nav.removeAllViews();listOf("الرئيسية" to "⌂","العملاء" to "♙","الحملات" to "✦","Inbox" to "◉","المزيد" to "☰").forEach{(n,i)->val x=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;background=shape(if(active==n)card2 else Color.TRANSPARENT);setOnClickListener{when(n){"الرئيسية"->dashboard();"العملاء"->contacts();"الحملات"->campaigns();"Inbox"->inbox();"المزيد"->more()}}};x.addView(t(i,19f,true).apply{gravity=Gravity.CENTER});x.addView(m(n,10f).apply{gravity=Gravity.CENTER});nav.addView(x,LinearLayout.LayoutParams(0,-1,1f))}}
    private fun page(a:String,s:String=""){body.removeAllViews();body.addView(t(a,26f,true));if(s.isNotBlank())body.addView(m(s,13f))}
    private fun section(s:String){body.addView(t(s,15f,true).apply{setPadding(2,dp(18),2,dp(8))})}
    private fun addBtn(s:String,action:()->Unit){val x=b(s,true);x.setOnClickListener{action()};body.addView(x,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,0,0,dp(8))})}
    private fun metric(a:String,v:String){val x=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=shape(card)};x.addView(m(a,11f));x.addView(t(v,23f,true));body.addView(x,LinearLayout.LayoutParams(0,dp(92),1f).apply{setMargins(dp(4),0,dp(4),dp(8))})}
    private fun dashboard(){active="الرئيسية";drawNav();page("Dashboard","إدارة العملاء والحملات والقنوات من مكان واحد.")
        val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("Contacts" to "1,248","Campaigns" to "18").forEach{metric(it.first,it.second)};body.removeViews(body.childCount-2,2)
        r.addView(cardMetric("Contacts","1,248"),LinearLayout.LayoutParams(0,dp(92),1f));r.addView(cardMetric("Campaigns","18"),LinearLayout.LayoutParams(0,dp(92),1f));body.addView(r)
        val r2=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};r2.addView(cardMetric("Sent","32.8K"),LinearLayout.LayoutParams(0,dp(92),1f));r2.addView(cardMetric("Replies","2.4K"),LinearLayout.LayoutParams(0,dp(92),1f));body.addView(r2)
        section("Quick actions");addBtn("＋ إنشاء حملة",::campaigns);addBtn("＋ إضافة عميل",::contacts)
        section("Performance");body.addView(m("Sent  32,800     Delivered  30,420     Read  26,180     Replies  2,410",13f))
    }
    private fun cardMetric(a:String,v:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=shape(card);addView(m(a,11f));addView(t(v,23f,true))}
    private fun contacts(){active="العملاء";drawNav();page("Contacts","Unified customer profiles • Tags • Segments");addBtn("⇩ استيراد CSV / TXT"){toast("CSV/TXT importer ready")};addBtn("＋ إضافة عميل"){dialogContact()};body.addView(t("بحث بالاسم أو الرقم أو Tag",13f).apply{background=shape(card2);setPadding(dp(14),dp(14),dp(14),dp(14))});section("1,248 Contacts");repeat(8){n->contactRow(if(n==0)"Ahmed Mohamed" else "Customer ${n+1}","20xxxxxxxxx  •  VIP  •  WhatsApp")}}
    private fun contactRow(a:String,c:String){val x=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(10),dp(14),dp(10));background=shape(card)};x.addView(t(a,14f,true));x.addView(m(c,11f));body.addView(x,LinearLayout.LayoutParams(-1,dp(70)).apply{setMargins(0,0,0,dp(7))})}
    private fun campaigns(){active="الحملات";drawNav();page("Campaigns","Create • Schedule • Automate • Track");addBtn("＋ New Campaign",::wizard);section("Running");camp("Ramadan Offer","WhatsApp • Running • 4,820 / 7,200");camp("Telegram Update","Telegram • Paused • 1,240 / 1,240");section("Recent");camp("Welcome Flow","WhatsApp • Completed • 3,480 / 3,480")}
    private fun camp(a:String,c:String){val x=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(12),dp(14),dp(12));background=shape(card)};x.addView(t(a,15f,true));x.addView(m(c,11f));val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("▶ Start","Ⅱ Pause","☷ Details").forEach{r.addView(b(it),LinearLayout.LayoutParams(0,dp(44),1f))};x.addView(r);body.addView(x,LinearLayout.LayoutParams(-1,dp(125)).apply{setMargins(0,0,0,dp(8))})}
    private fun wizard(){page("New Campaign","Audience → Channel → Message → Schedule");section("Audience");addBtn("All Contacts"){};addBtn("Segment: VIP"){};section("Channel");val s=Spinner(this);s.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("WhatsApp Business","Telegram","Instagram","Messenger"));body.addView(s);section("Message");val e=EditText(this).apply{hint="اكتب الرسالة... استخدم {{name}}";setHintTextColor(gray);setTextColor(white);minLines=5;gravity=Gravity.TOP;background=shape(card2);setPadding(dp(14),dp(12),dp(14),dp(12))};body.addView(e,LinearLayout.LayoutParams(-1,dp(135)));section("Schedule");addBtn("▶ Start now"){toast("Campaign queued")};addBtn("◷ Schedule"){toast("Schedule builder")}}
    private fun inbox(){active="Inbox";drawNav();page("Unified Inbox","WhatsApp • Telegram • Instagram • Messenger");section("Conversations");repeat(6){contactRow("Customer ${it+1}",if(it%2==0)"WhatsApp • محتاج تفاصيل العرض" else "Telegram • Thank you!")}}
    private fun more(){active="المزيد";drawNav();page("More","Control center");addBtn("⚡ Automations"){simple("Automations","Trigger → Condition → Action")};addBtn("▤ Templates"){simple("Templates","Reusable messages")};addBtn("◎ Channels"){channels()};addBtn("◈ Analytics"){analytics()};addBtn("☷ Logs"){simple("Logs","Delivery and system events")};addBtn("⚙ Settings"){simple("Settings","Workspace, backend and security")}}
    private fun simple(a:String,s:String){page(a,s);section("Rules");addBtn("＋ New"){toast("Builder opened")};camp("VIP Follow-up","Active • Trigger → Action")}
    private fun channels(){page("Channels","Official provider connections");camp("WhatsApp Business","Cloud API • Server-side credentials");camp("Telegram","Bot API • chat_id targets");camp("Instagram","Meta API • permissions required");camp("Messenger","Meta API • permissions required")}
    private fun analytics(){page("Analytics","Campaign performance");camp("Messages sent","32,800 • ↑ 18%");camp("Delivered","30,420 • 92.7%");camp("Read","26,180 • 86.1%");camp("Replies","2,410 • 7.3%")}
    private fun dialogContact(){val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),0,dp(18),0)};val n=EditText(this).apply{hint="الاسم"};val p=EditText(this).apply{hint="رقم الهاتف"};l.addView(n);l.addView(p);AlertDialog.Builder(this).setTitle("عميل جديد").setView(l).setNegativeButton("إلغاء",null).setPositiveButton("حفظ"){_,_->toast("تمت إضافة العميل")}.show()}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
