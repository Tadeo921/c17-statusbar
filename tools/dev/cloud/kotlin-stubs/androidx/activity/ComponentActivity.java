package androidx.activity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
public class ComponentActivity extends android.app.Activity implements LifecycleOwner {
    public Lifecycle getLifecycle() { throw new UnsupportedOperationException(); }
    public <I, O> ActivityResultLauncher<I> registerForActivityResult(ActivityResultContract<I, O> contract, ActivityResultCallback<O> callback) { throw new UnsupportedOperationException(); }
    public OnBackPressedDispatcher getOnBackPressedDispatcher() { throw new UnsupportedOperationException(); }
}
