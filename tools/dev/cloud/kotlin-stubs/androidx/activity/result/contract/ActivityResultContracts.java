package androidx.activity.result.contract;
public final class ActivityResultContracts {
    public static class OpenDocument extends ActivityResultContract<String[], android.net.Uri> { public OpenDocument() { } }
    public static class CreateDocument extends ActivityResultContract<String, android.net.Uri> { public CreateDocument(String mimeType) { } }
}
