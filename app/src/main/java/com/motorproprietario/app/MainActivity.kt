package com.motorproprietario.app
import android.os.Bundle
import android.graphics.Typeface
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity: AppCompatActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,70,32,32) }
  val title=TextView(this).apply { text="MOTOR PROPRIETÁRIO"; textSize=24f; setTypeface(null,Typeface.BOLD) }
  val status=TextView(this).apply {
   text="V171.0\n\nSmartphone Beta\nPaper Trading: ATIVO\nExecução real: DESATIVADA"
   textSize=18f; setPadding(0,30,0,30)
  }
  box.addView(title); box.addView(status)
  setContentView(box)
 }
}
