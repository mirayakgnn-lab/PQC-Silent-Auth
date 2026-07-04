package com.example.prototipuygulama;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        final Button btn = findViewById(R.id.checkButton);
        final Button wifiBtn = findViewById(R.id.wifiButton);
        final TextView txt = findViewById(R.id.statusText);
        final ProgressBar loader = findViewById(R.id.loadingBar);
        final TextView details = findViewById(R.id.simDetailText);

        txt.setVisibility(View.GONE);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

                // İZİN KONTROLÜ
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // İzin yoksa istenir
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, 101);
                    return;
                }

                //İnternet var mı?
                if (activeNetwork != null && activeNetwork.isConnected()) {

                    //Bağlantı tipi Hücresel Veri
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {

                        txt.setVisibility(View.VISIBLE);

                        // BAŞARI SENARYOSU: Sessiz Doğrulama Başlıyor
                        btn.setVisibility(View.GONE);
                        loader.setVisibility(View.VISIBLE);
                        txt.setText("Sessiz Ağ Doğrulaması yapılıyor...\n(PQC Katmanı Hazırlanıyor)");
                        txt.setTextColor(Color.BLUE);

                        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                        String simSerial = "Bilinmiyor";

                        // Android 10 ve üzeri için güvenlik kısıtlaması nedeniyle seri numarası yerine operatör adı okuyabiliriz.
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            simSerial = "Operatör: " + tm.getNetworkOperatorName();
                        } else {
                            simSerial = "SIM ID: " + tm.getSimSerialNumber(); // Eski sürümlerde ID'yi çeker
                        }

                        String simStatus;

                        if (tm.getSimState() == TelephonyManager.SIM_STATE_READY) {
                            // Seri numarası yerine "SIM Kart Hazır ve Aktif" bilgisini kullanıyoruz
                            simStatus = "SIM_OK_HASH_" + Integer.toHexString(tm.getNetworkOperatorName().hashCode());
                        } else {
                            simStatus = "SIM_ERROR";
                        }

                        long timestamp = System.currentTimeMillis(); // Timestamp is included to prevent replay attacks
                        final String finalSimData = simStatus + "|" + timestamp; // Bunu PQC şifreleme simülasyonunda kullanabilirsin

                        // ---------- POST-QUANTUM (KYBER) LAYER ----------
                        String operatorPublicKey = KyberKEM.getOperatorPublicKey();
                        KyberKEM.KyberResult kyberResult =
                                KyberKEM.encapsulate(operatorPublicKey, finalSimData);
                        String kyberSharedSecret = kyberResult.sharedSecret;

                        // ---------- HMAC LAYER ----------
                        String hmac = HMACUtils.generateHMAC(kyberSharedSecret, finalSimData);

                        // Verify HMAC (simulated operator check)
                        if (!HMACUtils.verifyHMAC(kyberSharedSecret, finalSimData, hmac)) {
                            txt.setText("HMAC Doğrulama Başarısız.");
                            loader.setVisibility(View.GONE);
                            return;
                        }

                        // Verify PQC session
                        if (!PQCUtils.verifyKyberSession(kyberSharedSecret)) {
                            txt.setText("Post-Quantum Doğrulama Başarısız.");
                            loader.setVisibility(View.GONE);
                            return;
                        }

                        final String finalSimSerial = simSerial;
                        final String operatorName = tm.getNetworkOperatorName();

                        txt.setText("Operatör (" + operatorName + ") üzerinden doğrulanıyor...");

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loader.setVisibility(View.GONE);
                                txt.setText("KİMLİK DOĞRULANDI\nHoş Geldiniz!");
                                txt.setTextColor(Color.parseColor("#2E7D32"));
                                details.setText("Operatör: " + operatorName + "\nSİM Durumu: " + simStatus);
                            }
                        }, 2000);
                    } else {
                        // HATA SENARYOSU: Wi-Fi Bağlı
                        txt.setVisibility(View.VISIBLE);
                        txt.setText("GÜVENLİK UYARISI:\nWi-Fi üzerinden doğrulama yapılamaz.\nLütfen Wi-Fi'yi kapatıp mobil ağa geçin.");
                        txt.setTextColor(Color.RED);
                        btn.setText("Tekrar Dene");
                        wifiBtn.setVisibility(View.VISIBLE);
                    }

                } else {
                    // HATA SENARYOSU: Wi-Fi Bağlı
                    txt.setVisibility(View.VISIBLE);
                    txt.setText("GÜVENLİK UYARISI:\nLütfen mobil ağa geçin.");
                    txt.setTextColor(Color.RED);
                    btn.setText("Tekrar Dene");
                }
            }
        });

        wifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn.setVisibility(View.GONE);
                txt.setText("Kullanıcının Wi-Fi kullanması durumunda doğrulama sistemi" +
                        " geleneksel SMS (OTP) Ağ Doğrulama Sistemine geçiş yapmaktadır." +
                        " Bu uygulamada SMS Ağ Doğrulama simüle edilmemektedir.");
                details.setVisibility(View.GONE);
                wifiBtn.setVisibility(View.GONE);

            }
        });

    }
}

/*
Modern Android sürümlerinde ICCID (Seri No) gibi donanımsal verilere erişim,
kullanıcı gizliliği için kısıtlanmıştır. Veriyi cihazdan çalmak yerine, operatörün Sessiz Ağ Doğrulaması (SNA) protokolü ile operatör
tarafında güvenli bir şekilde eşleştiriyoruz.
 */