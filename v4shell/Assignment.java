package v4shell;

import battlecode.common.ResourceType;

public class Assignment {

    public Assignment(int hqIdx, int num, int wellIdx) {
        this.hqIdx = hqIdx;
        this.num = num;
        this.wellIdx = wellIdx;
    }

    int hqIdx;
    int num;

    int wellIdx;

    public Assignment(int encoded) {
        num = encoded % 6;
        hqIdx = (encoded /= 6) % 4;
        wellIdx = encoded / 4;
    }

    public static int encodeAssignment(int wellIdx, int hqIdx, int num) {
        return (wellIdx * 4 + hqIdx) * 6 + num;
    }
    public int encodeAssignment() {
        return (wellIdx * 4 + hqIdx) * 6 + num;
    }

}
