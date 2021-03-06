package in.yuvi.ruleone;

import java.io.*;

import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import org.json.JSONObject;
import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import android.widget.Toast;
import in.yuvi.ruleone.functions.PrintFunction;

public class MainAcivity extends Activity {
    private WebView codeWebView;
	private TextView status;
	private LuaState L;

    private CommunicationBridge bridge;

	final StringBuilder output = new StringBuilder();

	private static byte[] readAll(InputStream input) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
		byte[] buffer = new byte[4096];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
		return output.toByteArray();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        codeWebView = (WebView) findViewById(R.id.codeWebView);
        bridge = new CommunicationBridge(codeWebView, "file:///android_asset/index.html");
        // Strip out for prod!
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            codeWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }

		status = (TextView) findViewById(R.id.statusText);
		status.setMovementMethod(ScrollingMovementMethod.getInstance());

		L = LuaStateFactory.newLuaState();
		L.openLibs();

		try {
			JavaFunction print = new PrintFunction(L, output);
			print.register("print");

			JavaFunction assetLoader = new JavaFunction(L) {
				@Override
				public int execute() throws LuaException {
					String name = L.toString(-1);

					AssetManager am = getAssets();
					try {
						InputStream is = am.open(name + ".lua");
						byte[] bytes = readAll(is);
						L.LloadBuffer(bytes, name);
						return 1;
					} catch (Exception e) {
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						e.printStackTrace(new PrintStream(os));
						L.pushString("Cannot load module "+name+":\n"+os.toString());
						return 1;
					}
				}
			};
			
			L.getGlobal("package");            // package
			L.getField(-1, "loaders");         // package loaders
			int nLoaders = L.objLen(-1);       // package loaders
			
			L.pushJavaFunction(assetLoader);   // package loaders loader
			L.rawSetI(-2, nLoaders + 1);       // package loaders
			L.pop(1);                          // package
						
			L.getField(-1, "path");            // package path
			String customPath = getFilesDir() + "/?.lua";
			L.pushString(";" + customPath);    // package path custom
			L.concat(2);                       // package pathCustom
			L.setField(-2, "path");            // package
			L.pop(1);
		} catch (Exception e) {
			status.setText("Cannot override print");
		}

        bridge.addListener("onCodeTextResponse", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                status.setText("");
                try {
                    String res = evalLua(messagePayload.optString("code"));
                    status.append(res);
                    status.append("Finished succesfully");
                } catch(LuaException e) {
                    Toast.makeText(MainAcivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
	}

	String evalLua(String src) throws LuaException {
		L.setTop(0);
		int ok = L.LloadString(src);
		if (ok == 0) {
			L.getGlobal("debug");
			L.getField(-1, "traceback");
			L.remove(-2);
			L.insert(-2);
			ok = L.pcall(0, 0, -2);
			if (ok == 0) {				
				String res = output.toString();
				output.setLength(0);
				return res;
			}
		}
		throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
		//return null;		
		
	}

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_execute:
                bridge.sendMessage("requestCodeText", new JSONObject());
                return true;
            default:
                throw new RuntimeException("Unknown menu item clicked");
        }
    }

    private String errorReason(int error) {
		switch (error) {
		case 4:
			return "Out of memory";
		case 3:
			return "Syntax error";
		case 2:
			return "Runtime error";
		case 1:
			return "Yield error";
		}
		return "Unknown error " + error;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}