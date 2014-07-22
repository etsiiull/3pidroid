
package es.ull.etsii.tfg.threepidroid;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.ull.etsii.tfg.threepidroid.usbserial.driver.UsbSerialPort;
import es.ull.etsii.tfg.threepidroid.usbserial.util.SerialInputOutputManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
/**
 * Esta clase es utilizada para la interfaz principal de la aplicación, 
 * siendo activada cuando un dispositivo es reconocido en la clase
 * DeviceListActivity.
 * 
 * Utiliza las clases CameraPreview.java y ResizableCameraPreview.Java
 * Y se comunica por serial con el 3pi utilizando la librería 
 * Android Usb Serial, incluida en el proyecto mediante sus fuentes.
 *
 */
public class CameraPreviewTestActivity extends Activity implements AdapterView.OnItemSelectedListener {
	private final String TAG = CameraPreviewTestActivity.class.getSimpleName();

	private ResizableCameraPreview mPreview;
	private ArrayAdapter<String> mAdapter;
	private RelativeLayout mLayout;
	private TextView view10;
	private TextView view45;
	private TextView view90;
	private TextView view135;
	private TextView view170;
	private CheckBox checktelemetria;
	private int mCameraId = 0;
	//    private UsbSerialPort port;
	private static UsbSerialPort sPort = null;
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private SerialInputOutputManager mSerialIoManager;
	private final SerialInputOutputManager.Listener mListener =
			new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d(TAG, "Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			CameraPreviewTestActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					CameraPreviewTestActivity.this.updateReceivedData(data);
				}
			});
		}
	};

	private int checksum1;
	private int checksum2;
	private int[] pack;
	private int lenght;

	AlertDialog alertDialog;


	/*
	 * Se llama al método show desde otra clase para que
	 * la activity de la esta clase se convierta en la actual.
	 */
	static void show(Context context, UsbSerialPort port) {
		sPort = port;
		final Intent intent = new Intent(context, CameraPreviewTestActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
		context.startActivity(intent);
	}

	static void shownoport(Context context) {
		sPort = null;
		final Intent intent = new Intent(context, CameraPreviewTestActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
		context.startActivity(intent);
	}

	/*
	 * Este método se activa cada vez que el hilo que espera
	 * datos por serial recibe algun byte.
	 */
	private void updateReceivedData(byte[] data) {
		@SuppressWarnings("unused")
		String receptor = "" + data.length + " bytes, " + Byte.toString(data[0]);
		//alertDialog.setMessage(receptor);
		//alertDialog.show();
	}


	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Hide status-bar
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Hide title-bar, must be before setContentView
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.test);
		checktelemetria = (CheckBox)findViewById(R.id.checkBox1);
		view10 = (TextView)findViewById(R.id.textView10g);
		view45 = (TextView)findViewById(R.id.textView45g);
		view90 = (TextView)findViewById(R.id.textView90g);
		view135 = (TextView)findViewById(R.id.textView135g);
		view170 = (TextView)findViewById(R.id.textView170g);
		view10.setVisibility(View.INVISIBLE);
		view45.setVisibility(View.INVISIBLE);
		view90.setVisibility(View.INVISIBLE);
		view135.setVisibility(View.INVISIBLE);
		view170.setVisibility(View.INVISIBLE);

		checktelemetria.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
				if (isChecked) {
					view10.setVisibility(View.VISIBLE);
					view45.setVisibility(View.VISIBLE);
					view90.setVisibility(View.VISIBLE);
					view135.setVisibility(View.VISIBLE);
					view170.setVisibility(View.VISIBLE);
				} else {
					view10.setVisibility(View.INVISIBLE);
					view45.setVisibility(View.INVISIBLE);
					view90.setVisibility(View.INVISIBLE);
					view135.setVisibility(View.INVISIBLE);
					view170.setVisibility(View.INVISIBLE);
				}
			}
		});

		// Spinner for preview sizes
		Spinner spinnerSize = (Spinner) findViewById(R.id.spinner_size);
				mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
				mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinnerSize.setAdapter(mAdapter);
				spinnerSize.setOnItemSelectedListener(this);

				// Spinner for camera ID
				Spinner spinnerCamera = (Spinner) findViewById(R.id.spinner_camera);
				ArrayAdapter<String> adapter;
				adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinnerCamera.setAdapter(adapter);
				spinnerCamera.setOnItemSelectedListener(this);
				adapter.add("Frontal");
				adapter.add("Retrovisor");
				//adapter.add("Frontal 2");

				mLayout = (RelativeLayout) findViewById(R.id.layout);


				alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("Mensaje");
				alertDialog.setMessage("Texto");
				alertDialog.setButton("Aceptar", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				});



	}

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		Log.w("CameraPreviewTestActivity", "onItemSelected invoked");
		Log.w("CameraPreviewTestActivity", "position: " + position);
		Log.w("CameraPreviewTestActivity", "parent.getId(): " + parent.getId());
		switch (parent.getId()) {
		case R.id.spinner_size:
			Rect rect = new Rect();
			mLayout.getDrawingRect(rect);

			if (0 == position) { // "Auto" selected
				mPreview.surfaceChanged(null, 0, rect.width(), rect.height());
			} else {
				mPreview.setPreviewSize(position - 1, rect.width(), rect.height());
			}
			break;
		case R.id.spinner_camera:
			mPreview.stop();
			mLayout.removeView(mPreview);
			mCameraId = position;
			createCameraPreview();
			break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// do nothing
	}

	protected void onResume() {
		super.onResume();
		createCameraPreview();
		alertDialog.setMessage("Resumed, port=" + sPort);
		alertDialog.show();
		Log.d(TAG, "Resumed, port=" + sPort);
		if (sPort == null) {
			Log.d(TAG, "No serial Device");
			alertDialog.setMessage("No serial Device");
			alertDialog.show();
		} else {


			UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
			UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());


			if (connection == null) {
				alertDialog.setMessage("Opening Device Failed " + sPort.getDriver().getDevice().toString());
				alertDialog.show();
				return;
			}

			try {
				sPort.open(connection);
				sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
			} catch (IOException e) {
				Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
				alertDialog.setMessage("Error opening device: " + e.getMessage());
				alertDialog.show();
				try {
					sPort.close();
				} catch (IOException e2) {
					// Ignore.
				}
				sPort = null;
				return;
			}
			//            alertDialog.setMessage("Serial device: " + sPort.getClass().getSimpleName());
			//            alertDialog.show();
		}
		onDeviceStateChange();

	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "Stopping io manager ..");
			mSerialIoManager.stop();
			mSerialIoManager = null;
		}
	}

	private void startIoManager() {
		if (sPort != null) {
			Log.i(TAG, "Starting io manager ..");
			mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
			mExecutor.submit(mSerialIoManager);
		}
	}

	private void onDeviceStateChange() {
		stopIoManager();
		startIoManager();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPreview.stop();
		mLayout.removeView(mPreview);
		mPreview = null;
		if (sPort != null) {
			try {
				sPort.close();
			} catch (IOException e) {
				// Ignore.
			}
			sPort = null;
		}
		finish();
	}

	/*
	 * Método para pasar un entero decimal a formato
	 * binario, depositando el resultado en un string.
	 */
	public String int2bin (int dec) {
		if (dec == 0)
			return "0";
		String dig;
		String bin = "";

		while (dec > 0) {
			if (dec % 2 == 1)
				dig = "1";
			else
				dig = "0";
			bin = dig + bin;
			dec /= 2;
		}
		return bin;
	}

	/*
	 * Método para calcular el checksum, analizando la estructura
	 * del paquete.	
	 */
	public void calcChsum() {
		int c = 0;
		int n = lenght - 2;
		int posicion = 0;

		while (n > 1) {
			c = c + ((pack[posicion]<<8) | (pack[(posicion + 1)]));
			c = c & Integer.parseInt("ffff", 16);
			n = n - 2;
			posicion = posicion + 2;
		}
		if (n > 0)
			c = c ^ pack[(posicion + 1)];

		System.out.println("El checksum es: " + c);
		checksum1 = c>>>8;
		checksum2 = c & Integer.parseInt("00ff", 16);
		System.out.println(checksum1);
		System.out.println(checksum2);
	}

	/*
	 * Inicializa la pantalla donde se muestra lo que ve la camara.
	 */
	private void createCameraPreview() {
		// Set the second argument by your choice.
		// Usually, 0 for back-facing camera, 1 for front-facing camera.
		// If the OS is pre-gingerbreak, this does not have any effect.
		mPreview = new ResizableCameraPreview(this, mCameraId, CameraPreview.LayoutMode.FitToParent, false);
		LayoutParams previewLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mLayout.addView(mPreview, 0, previewLayoutParams);

		mAdapter.clear();
		mAdapter.add("Auto");
		List<Camera.Size> sizes = mPreview.getSupportedPreivewSizes();
		for (Camera.Size size : sizes) {
			mAdapter.add(size.width + " x " + size.height);
		}
	}

	/*
	 * Envia el byte "100"(decimal) que corresponde al comando acelerar
	 * en modo directo para el 3pidroid.
	 */
	public void acelerar(View view) throws IOException {
		String a1 = "1100100";
		byte[] b = new byte[1];
		b[0] = (byte)Integer.parseInt(a1, 2);
		mSerialIoManager.writeAsync(b);  	
	}

	/*
	 * Envia el byte "111"(decimal) que corresponde al comando desacelerar
	 * en modo directo para el 3pidroid.
	 */
	public void decelerar(View view) throws IOException {
		String a1 = "1101111";
		byte[] b = new byte[1];
		b[0] = (byte)Integer.parseInt(a1, 2);
		mSerialIoManager.writeAsync(b);  
	}

	/*
	 * Envia el byte "122"(decimal) que corresponde al comando girar
	 * a la izquierda en modo directo para el 3pidroid.
	 */
	public void girarizq(View view) throws IOException {
		String a1 = "1111010";
		byte[] b = new byte[1];
		b[0] = (byte)Integer.parseInt(a1, 2);
		mSerialIoManager.writeAsync(b);
	}

	/*
	 * Envia el byte "85"(decimal) que corresponde al comando girar
	 * a la izquierda ligeramente en modo directo para el 3pidroid.
	 */
	public void girarizqs(View view) throws IOException {
		String a1 = "1010101";
		byte[] b = new byte[1];
		b[0] = (byte)Integer.parseInt(a1, 2);
		mSerialIoManager.writeAsync(b);
	}

	/*
	 * Envia el byte "127"(decimal) que corresponde al comando girar
	 * a la derecha en modo directo para el 3pidroid.
	 */
	public void girarder(View view) throws IOException {
		String a1 = "111111";
		byte[] b = new byte[1];
		b[0] = (byte)Integer.parseInt(a1, 2);
		mSerialIoManager.writeAsync(b);
	}

	/*
	 * Envia el byte "60"(decimal) que corresponde al comando girar
	 * a la derecha ligeramente en modo directo para el 3pidroid.
	 */
	public void girarders(View view) throws IOException {
		String a1 = "111100";
		byte[] b = new byte[1];
		b[0] = (byte)Integer.parseInt(a1, 2);
		mSerialIoManager.writeAsync(b);
	}
	
	/*
	 * Envia el byte "70"(decimal) que corresponde al comando parar
	 * en seco en modo directo para el 3pidroid.
	 */
	public void parartotal(View view) throws IOException {
		String a1 = "1000110";
		byte[] b = new byte[1];
		b[0] = (byte)Integer.parseInt(a1, 2);
		mSerialIoManager.writeAsync(b);
	}
}
