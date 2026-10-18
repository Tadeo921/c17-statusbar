package androidx.activity
class SystemBarStyle private constructor() {
    companion object {
        fun dark(scrim: Int): SystemBarStyle = SystemBarStyle()
        fun light(scrim: Int, darkScrim: Int): SystemBarStyle = SystemBarStyle()
        fun auto(lightScrim: Int, darkScrim: Int): SystemBarStyle = SystemBarStyle()
    }
}
fun ComponentActivity.enableEdgeToEdge(statusBarStyle: SystemBarStyle = SystemBarStyle.auto(0, 0), navigationBarStyle: SystemBarStyle = SystemBarStyle.auto(0, 0)) { }
