import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;

import java.util.List;

import static java.util.Arrays.asList;

@Getter
public class Properties {
    private static Properties INSTANCE;
    private final String argosServiceBaseUrl;
    private final String passPhrase;
    private final String keyId;
    private final String supplyChainName;
    private final List<String> path;
    private final String workspace;

    public static Properties getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Properties();
        }
        return INSTANCE;
    }

    private Properties() {
        Config conf = ConfigFactory.load();
        if (System.getenv("ARGOS_SERVICE_BASE_URL") == null) {
            argosServiceBaseUrl = conf.getString("argos-service.rest-api.base-url");
        } else {
            argosServiceBaseUrl = System.getenv("ARGOS_SERVICE_BASE_URL");
        }
        if (System.getenv("CREDENTIALS_PASSPHRASE") == null) {
            passPhrase = conf.getString("supplychain.credentials.passphrase");
        } else {
            passPhrase = System.getenv("CREDENTIALS_PASSPHRASE");
        }
        if (System.getenv("CREDENTIALS_KEY_ID") == null) {
            keyId = conf.getString("supplychain.credentials.keyid");
        } else {
            keyId = System.getenv("CREDENTIALS_KEY_ID");
        }
        if (System.getenv("SUPPLY_CHAIN_PATH") == null) {
            path = asList(conf.getString("supplychain.path").split("\\."));
        } else {
            path = asList(System.getenv("SUPPLY_CHAIN_PATH").split("\\."));
        }
        if (System.getenv("SUPPLY_CHAIN_NAME") == null) {
            supplyChainName = conf.getString("supplychain.name");
        } else {
            supplyChainName = System.getenv("SUPPLY_CHAIN_NAME");
        }
        if (System.getenv("WORKSPACE") == null) {
            workspace = conf.getString("workspace");
        } else {
            workspace = System.getenv("WORKSPACE");
        }
    }
}
