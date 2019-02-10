package fr.angers.univ.qrludo.QR.model;

import com.google.gson.internal.LinkedTreeMap;

import fr.angers.univ.qrludo.exceptions.UnhandledQRException;


/**
 * Created by Florian Lherbeil
 */


/**
 * Represents the information of a QRCodeAtomique.
 */
public class QRCodeAtomique extends QRCode {

    /**
     *
     * @param code QrCode Json obtenu dans la classe QRCodeBuilder
     * @param rawValue valeur initiale contenue dans le qrcode
     * @throws UnhandledQRException
     */
    public QRCodeAtomique(QrCodeJson code,String rawValue) throws UnhandledQRException {
        super(code,rawValue);
        FileJson music=new FileJson();
        for(Object data : code.getData()){
            if(isUrlFile(data.toString())) {
                if (data instanceof LinkedTreeMap) {
                    music = createJsonFile((LinkedTreeMap) data);
                    if (music.getType().equalsIgnoreCase("music")) {
                        String url = music.getUrl();
                        m_content.add(new QRFile(url));
                    }
                }
            }
            else {
                m_content.add(new QRText(data.toString()));
            }
        }

    }

}