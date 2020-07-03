package it.eng.idsa.businesslogic.util.config.keystore;

import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * @author Milan Karajovic and Gabriele De Luca
 */
@Component
public class TruststoreConfig {

    //TODO Application properties
    public SSLContextParameters setupSSLContextParameters() throws IOException {
        KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
        // Change this path to point to your truststore/keystore as jks files

        final URL resource = getClass().getClassLoader().getResource("aisecconnector1-keystore.jks");
        keyStoreParameters.setResource(resource.getPath());
        keyStoreParameters.setPassword("password");

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        keyManagersParameters.setKeyStore(keyStoreParameters);

        keyManagersParameters.setKeyPassword("password");

        KeyStoreParameters trustStoreParameters = new KeyStoreParameters();
        final URL resource2 = getClass().getClassLoader().getResource("client-truststore_new.jks");
        trustStoreParameters.setResource(resource2.getPath());
        trustStoreParameters.setPassword("password");

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setKeyStore(trustStoreParameters);

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(keyManagersParameters);
        sslContextParameters.setTrustManagers(trustManagersParameters);
        return sslContextParameters;
    }

    public SSLSocketFactory getSslSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                }
        };
        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        // Create an ssl socket factory with our all-trusting manager
        return sslContext.getSocketFactory();
    }
}

