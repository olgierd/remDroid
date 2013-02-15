package es.olgierd.remdroid;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RemDroidActivity extends Activity {
    /** Called when the activity is first created. */

    Button startBtn, searchBtn;
    TextView ipField;
    String ipAddrString;
    Context context;

    ArrayList<InetAddress> serwery = new ArrayList<InetAddress>();
    private LinearLayout ll;

    public void startSending(String ipAddr) {
	// wywołuje Activity które transmituje dane na IP podany jako parametr
	
	Intent intent = new Intent(this, DataSendActivity.class);
	intent.putExtra("ipAddr", ipAddr);
	
	this.startActivity(intent);

    }

    private class IPAddressValidator {
// sprawdza poprawność adresu IP wprowadzonego do pola tekstowego
	
	private Pattern pattern;
	private Matcher matcher;

	private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
		+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	public IPAddressValidator() {
	    pattern = Pattern.compile(IPADDRESS_PATTERN);
	}

	public boolean validate(final String ip) {
	    matcher = pattern.matcher(ip);
	    return matcher.matches();
	}
    }

    private class SearchForServer extends AsyncTask<Void, Integer, Void> {
// wątek szukający serwerów
	
	
	private InetAddress broadcast;
	private DatagramPacket dp = null;
	public DatagramSocket ds = null;
	public boolean enabled = true;
	private String oldButtonText;

	private InetAddress getBroadcastAddress() {
	    // funkcja znajdująca adres broadcast dla aktywnego połączenia

	    Enumeration<NetworkInterface> interfaces = null;
	    try {
		interfaces = NetworkInterface.getNetworkInterfaces();
	    } catch (SocketException e1) {
		e1.printStackTrace();
	    }
	    while (interfaces.hasMoreElements()) {
		NetworkInterface networkInterface = interfaces.nextElement();
		try {
		    if (networkInterface.isLoopback())
			continue;
		} catch (SocketException e) {
		    e.printStackTrace();
		}

		for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
		    broadcast = interfaceAddress.getBroadcast();
		    if (broadcast == null)
			continue;
		}
	    }

	    return broadcast;

	}

	@Override
	protected void onPreExecute() {
	    
	    serwery.clear();
	    oldButtonText = searchBtn.getText().toString();
	    searchBtn.setEnabled(false);
	    searchBtn.setText("Searching...");

	    ll.removeAllViews();

	}

	@Override
	protected void onProgressUpdate(Integer... progress) {

	    if (progress[0] == 1) {
		TextView tv = new TextView(getApplicationContext());
		tv.setText("Sprawdź połączenie sieciowe");
		ll.addView(tv);
	    }

	}

	@Override
	protected void onPostExecute(Void v) {

	    addButtonsFromList();
	    searchBtn.setEnabled(true);
	    searchBtn.setText(oldButtonText);

	}

	@Override
	protected Void doInBackground(Void... params) {

	    byte[] pack = new byte[5];

	    // pobieramy broadcast
	    InetAddress bcast = getBroadcastAddress();

	    // brak broadcastu -> brak połaczenia sieciowego
	    if (bcast == null) {

		publishProgress(1);
		return null;

	    }
	    
	    pack[4] = Configuration.PACKET_PING;

	    // przygotowujemy pakiet do wysłania
	    dp = new DatagramPacket(pack, pack.length, bcast, 22222);

	    try {
		// ustawiamy socket
		ds = new DatagramSocket(22224);
		// czas nasłuchu: 2 sekundy po każdym odebranym pakiecie
		ds.setSoTimeout(2000);
		// wysyłamy pakiet na broadcast (pusty)
		ds.send(dp);
	    } catch (SocketException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }

	    // czekamy na odpowiedzi
	    while (enabled) {
		try {
		    ds.receive(dp);
		    // dodajemy adres źródłowy każdego odebranego pakietu do listy
		    serwery.add(dp.getAddress());
		} catch (IOException e) {
		    e.printStackTrace();
		    break;
		}

	    }

	    ds.close();
	    return null;
	}

    }

    private void addButtonsFromList() {
// dodaje buttony z listy
	if (serwery.size() > 0) {

	    for (InetAddress ia : serwery) {
		addHostButton(ia.getHostAddress());
	    }

	}

	if (serwery.isEmpty()) {		// jeżeli nie znaleziono żadnych serwerów [bo. np. wycięty jest ruch przez Broadcast]
	    
	    // dodaje tekst:
	    TextView tv = new TextView(getApplicationContext());
	    tv.setText("Nie znaleziono serwerów. Wprowadź adres IP ręcznie:");

	    // pole na ręczne wpisanie IP
	    final EditText et = new EditText(getApplicationContext());

	    // button "POŁĄCZ"
	    Button btn = new Button(getApplicationContext());
	    btn.setText("Połącz");
	    btn.setOnClickListener(new View.OnClickListener() {

		@Override
		public void onClick(View v) {

		    IPAddressValidator validator = new IPAddressValidator();

		    if (!validator.validate(et.getText().toString())) {
			AlertDialog alertDialog = new AlertDialog.Builder(RemDroidActivity.this).create();
			alertDialog.setTitle("Adres IP");
			alertDialog.setMessage("Wprowadź poprawny adres IP.");
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) {
			    }
			});
			alertDialog.show();
		    }

		    else {
			
			startSending(et.getText().toString());
			
		    }
		    
		}

	    });

	    ll.addView(tv);
	    ll.addView(et);
	    ll.addView(btn);
	}

    }

    public void addHostButton(final String ipAddress) {
//	dodaje button ze znalezionym adresem serwera

	Button btn = new Button(getApplicationContext());
	btn.setText(ipAddress);

	btn.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {

		startSending(ipAddress);

	    }
	});

	ll.addView(btn);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);

	context = getApplicationContext();
	
	ll = (LinearLayout) findViewById(R.id.linbuttons);
	searchBtn = (Button) findViewById(R.id.button2);
	
	

	searchBtn.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {

		new SearchForServer().execute();

	    }
	});

    }

}
