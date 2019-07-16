package client.player.buffs;

import java.io.Serializable;

public class DiseaseValueHolder implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    public long startTime;
    public long length;
    public Disease disease;

    public DiseaseValueHolder(final Disease disease, final long startTime, final long length) {
        this.disease = disease;
        this.startTime = startTime;
        this.length = length;
    }
}

