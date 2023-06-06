package com.example.bluetootver3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    String TAG = "MainActivity";
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // HM-10 모듈의 UUID
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 1002;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_ENABLE_BT = 1;

    private static final int MESSAGE_READ = 1; // 예시로 사용한 메시지 식별자
    private TextView btReadings;
    private BluetoothSocket socket;
    TextView textStatus;
    Button btnPaired, btnSearch, btnSend;
    ListView listView;
    private EditText editMessage;
    BluetoothAdapter btAdapter;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;
    BluetoothSocket btSocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        btReadings = findViewById(R.id.text);
        editMessage = findViewById(R.id.edit_message);
        textStatus = findViewById(R.id.text_status);
        btnPaired = findViewById(R.id.btn_paired);
        btnSearch = findViewById(R.id.btn_search);
        listView = findViewById(R.id.listview);
        Button btnWrite = findViewById(R.id.btn_write);
        Button disconnect = findViewById(R.id.disconnect);

        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnect();
            }
        });

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPairedDevices();
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchDevices();
            }
        });

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editMessage.getText().toString();

                if (TextUtils.isEmpty(message)) {
                    // 메시지가 비어있으면 예외 문구를 표시합니다.
                    Toast.makeText(MainActivity.this, "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    // 메시지를 전송합니다.
                    sendMessage(message);
                }
            }
        });

        // 블루투스 어댑터 초기화
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
            return;
        }

        // 페어링 된 디아비스 보여주는 코드
        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        listView.setOnItemClickListener(new myOnItemClickListener());
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String deviceAddress = deviceAddressArray.get(position);
                connectToDevice(deviceAddress);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
        } else {
            initializeBluetooth();
        }
    }

    private String getBluetoothDeviceAddress() {
        // Bluetooth 장치 주소를 얻어오는 로직을 추가합니다.
        // 이전에 설명한 방법 중 하나를 사용하여 주소를 확인하고 반환합니다.
        // 예를 들어, 사용자에게 주소를 입력하도록 요청하는 UI를 만들어 입력값을 반환하거나,
        // 기기에서 저장된 주소 목록을 표시하고 사용자가 선택하도록 하는 등의 방법을 사용할 수 있습니다.
        // 이 예시에서는 임의의 주소를 반환하도록 하겠습니다.
        return "00:11:22:33:AA:BB";
    }

    private void initializeBluetooth() {
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            showPairedDevices();
        }
    }

    private void showPairedDevices() {
        btArrayAdapter.clear();
        deviceAddressArray.clear();

        if (btAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    btArrayAdapter.add(deviceName);
                    deviceAddressArray.add(deviceAddress);
                }
            }
        } else {
            Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchDevices() {
        btArrayAdapter.clear();
        deviceAddressArray.clear();

        if (btAdapter.isEnabled()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
            btAdapter.startDiscovery();
        } else {
            Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_BLUETOOTH_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 블루투스 권한이 승인된 경우
                    Toast.makeText(this, "Bluetooth permission granted", Toast.LENGTH_SHORT).show();
                    // BluetoothAdapter 객체를 가져옴
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                    // 블루투스가 지원되지 않는 경우
                    if (mBluetoothAdapter == null) {
                        Toast.makeText(this, "This device does not support Bluetooth", Toast.LENGTH_SHORT).show();
                    }
                    // 블루투스가 지원되는 경우
                    else {
                        // 블루투스가 꺼져있는 경우 블루투스를 켜기 위해 사용자에게 요청함
                        if (!mBluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        }
                        // 블루투스가 켜져 있는 경우
                        else {
                            Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    // 블루투스 권한이 거부된 경우
                    Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_ENABLE_BT:
                if (grantResults.length > 0 && grantResults[0] == RESULT_OK) {
                    // 블루투스가 켜진 경우
                    Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // 블루투스가 켜지지 않은 경우
                    Toast.makeText(this, "Bluetooth could not be enabled", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //Bluetooth를 활성화하는 요청에 대한 응답을 처리 코드
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is enabled, proceed with device connection
                String deviceAddress = null;
                connectToDevice(null);
            } else {
                // User denied enabling Bluetooth, handle the error
                Toast.makeText(this, "Failed to enable Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 블루투스 장치에 연결하기 위한 메서드
    private void connectToDevice(String deviceAddress) {
        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);

        // Ensure that Bluetooth is enabled
        if (!btAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            return;
        }

        // Cancel discovery if it is still running
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }

        BluetoothSocket socket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
            Toast.makeText(getApplicationContext(), "장치에 연결되었습니다.", Toast.LENGTH_SHORT).show();
            String deviceName = device.getName();
            textStatus.setText(deviceName);
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "장치에 연결할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // Bluetooth 기기를 탐색하여 그 정보를 받아 오는 코드
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceAddress);
            }
        }
    };

    private void disconnect() {
        if (connectedThread != null) {
            connectedThread.cancel(); // Close the connection
            connectedThread = null;
        }
        try {
            if (socket != null) {
                socket.close(); // Close the Bluetooth socket
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(getApplicationContext(), "장치와 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show();
        textStatus.setText(""); // Clear the status text
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, filter);
        registerBluetoothStateReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        unregisterBluetoothStateReceiver();
    }

    //현재 Bluetooth 상태를 확인하고 해당 상태에 따라 수행하는 코드
    private BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        // Bluetooth가 꺼진 경우, 상태 변경 처리 작업 수행
                        // 예시: Bluetooth 기능을 활성화하는 다이얼로그를 표시하거나 처리를 중지할 수 있음
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                        dialogBuilder.setTitle("Bluetooth 비활성화");
                        dialogBuilder.setMessage("Bluetooth를 활성화하시겠습니까?");
                        dialogBuilder.setPositiveButton("활성화", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Bluetooth 활성화를 위한 인텐트 생성
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }
                                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                            }
                        });
                        dialogBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Bluetooth 기능 처리를 중지하거나 앱 종료 등을 수행할 수 있음
                                finish();
                            }
                        });
                        AlertDialog dialog = dialogBuilder.create();
                        dialog.show();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        // Bluetooth가 켜진 경우, 상태 변경 처리 작업 수행
                        // 예시: Bluetooth 기기를 스캔하거나 연결 작업을 수행할 수 있음
                        Toast.makeText(MainActivity.this, "Bluetooth가 활성화되었습니다.", Toast.LENGTH_SHORT).show();
                        // Bluetooth 기기 스캔을 시작하거나 연결 작업을 수행하는 로직 추가
                        break;
                }
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                textStatus.setText("연결됨");
                Log.d(TAG, "Connected to GATT server.");
                // 연결 성공 시, 서비스 검색을 시작
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
                // 연결이 끊겼을 때 필요한 처리 작업 수행
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered successfully.");
                // 필요한 서비스와 특성을 찾아서 작업 수행
                // 예시: gatt.getService(serviceUUID).getCharacteristic(characteristicUUID).setValue(value);
                // 예시: gatt.writeCharacteristic(characteristic);
            } else {
                Log.w(TAG, "Service discovery failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read successfully.");
                // 특성 읽기 작업 완료 후 데이터 처리
                // 예시: byte[] data = characteristic.getValue();
            } else {
                Log.w(TAG, "Characteristic read failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic write successfully.");
                // 특성 쓰기 작업 완료 후 처리 작업
            } else {
                Log.w(TAG, "Characteristic write failed with status: " + status);
            }
        }
    };

    // 블루투스 상태 변경을 감지하기 위해 BroadcastReceiver 등록
    private void registerBluetoothStateReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
    }


    // 블루투스 상태 변경 감지를 중지하기 위해 BroadcastReceiver 등록 해제
    private void unregisterBluetoothStateReceiver() {
        unregisterReceiver(bluetoothStateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Bluetooth 연결 종료
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }

    private ConnectedThread connectedThread;

    //통신 부분
    private class myOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!btAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "Bluetooth가 비활성화 상태입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);

            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Toast.makeText(getApplicationContext(), "이미 페어링된 장치입니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "새로운 페어링을 시작합니다.", Toast.LENGTH_SHORT).show();
                pairDevice(device);
            }
        }
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        if (connectedThread != null) {
            connectedThread.write(message.getBytes());
        }
        editMessage.setText("");
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // 받은 메시지를 처리하는 로직을 추가합니다.
                    // 이 부분은 앱의 특정 요구사항에 따라 다르게 구현될 수 있습니다.
                    // 아래 코드는 받은 메시지를 UI에 표시하는 예시입니다.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btReadings.setText(readMessage);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


