package es.olgierd.remdroid;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class DataSendActivity extends Activity {
    /** Called when the activity is first created. */

    static final int scrollThreshold = 3;

    private TextView tv2;
    private DatagramSocket ds;
    private InetAddress ia;
    private DatagramPacket dp;
    private String ip_raw;
    private int dY, lasty = -1;

    // blokujemy możliwość przypadkowego zamknięcia aplikacji
    @Override
    public void onBackPressed() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.menu, menu);
	return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
	// Handle item selection
	switch (item.getItemId()) {
	case R.id.item1:
	    new SendPacketTask().execute(0, 0, Configuration.PACKET_BYEBYE);
	    this.finish();
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    // wątek służący do wysłania pakietu
    private class SendPacketTask extends AsyncTask<Integer, Integer, Integer> {

	@Override
	protected Integer doInBackground(Integer... params) {

	    byte[] pack = new byte[5];

	    // pakowanie intów do byte'ów
	    pack[0] = params[0].byteValue();
	    params[0] >>= 8;
	    pack[1] = params[0].byteValue();
	    pack[2] = params[1].byteValue();
	    params[1] >>= 8;
	    pack[3] = params[1].byteValue();

	    pack[4] = params[2].byteValue();

	    dp = new DatagramPacket(pack, pack.length, ia, 22222);

	    try {
		ds.send(dp);
	    } catch (IOException e) {
		e.printStackTrace();
	    }

	    return null;
	}

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	// zapobiegamy gaśnięciu ekranu, gdy aplikacja jest włączona
	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	// definiujemy wygląd okna - fullscreen
	requestWindowFeature(Window.FEATURE_NO_TITLE);
	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	setContentView(R.layout.blank);

	tv2 = (TextView) findViewById(R.id.text2);

	try {

	    ip_raw = getIntent().getExtras().getString("ipAddr");
	    ds = new DatagramSocket();
	    ia = InetAddress.getByName(ip_raw);
	    new SendPacketTask().execute(0, 0, Configuration.PACKET_HELLO);
	    
	} catch (SocketException e) {
	    e.printStackTrace();
	} catch (UnknownHostException e) {
	    e.printStackTrace();
	}

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	// obsługa dotknięcia

	// pobieramy lokację
	int x = (int) event.getX();
	int y = (int) event.getY();

	switch (event.getAction()) {

	// dotknięcie
	case MotionEvent.ACTION_DOWN: {

	    new SendPacketTask().execute(x, y, Configuration.PACKET_MOUSE_DOWN);
	    tv2.setText("DOWN");
	    break;
	}

	// przesunięcie
	case MotionEvent.ACTION_MOVE: {

	    // jednym palcem
	    if (event.getPointerCount() == 1) {
		new SendPacketTask().execute(x, y, Configuration.PACKET_MOUSE_MOVE);
		tv2.setText("MOVE");
	    }

	    // dwoma palcami - przewijanie
	    if (event.getPointerCount() == 2) {

		tv2.setText("SCROLL");

		dY = (int) (event.getY(0) + event.getY(1)) / 2;

		// zapamiętujemy średnie położenie palców w osi Y
		if (lasty == -1) {
		    lasty = dY;
		}

		// wysyłamy ilość pakietów proporcjonalną do tego o ile pikseli
		// przejechaliśmy w górę/dół
		// od pierwotnej lokalizacji

		if (dY > lasty + scrollThreshold) {
		    for (int i = 0; i <= Math.abs(dY - (lasty + scrollThreshold)) / 10; i++) {
			new SendPacketTask().execute(x, y, Configuration.PACKET_SCROLL_DOWN);
		    }
		    lasty = -1;
		}

		if (dY < lasty - scrollThreshold) {
		    for (int i = 0; i <= Math.abs(dY - (lasty - scrollThreshold)) / 10; i++) {
			new SendPacketTask().execute(x, y, Configuration.PACKET_SCROLL_UP);
		    }

		    lasty = -1;
		}

	    }

	    break;
	}

	// podniesienie palca
	case MotionEvent.ACTION_UP: {

	    new SendPacketTask().execute(x, y, Configuration.PACKET_MOUSE_UP);
	    lasty = -1;
	    tv2.setText("UP");
	    break;
	}

	}

	return false;

    }

}