package utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataFormat {

    private List<DataPair> formatList;
    private int totalByteLength;

    public DataFormat() {
        this.formatList = new ArrayList<>();
        this.totalByteLength = 0;
    }

    public DataFormat add(DataType type, int count) {
        formatList.add(new DataPair(type, count));
        totalByteLength += DataType.getByteSize(type) * count;
        return this;
    }

    public Iterator<DataPair> getFormatIterator() {
        return formatList.iterator();
    }

    public int getTotalByteLength() {
        return totalByteLength;
    }

    public enum DataType {
        BYTE, SHORT, INT, FLOAT, DOUBLE;

        public static int getByteSize(DataType type) {
            switch (type) {
                case BYTE: return 1;
                case SHORT: return 2;
                case INT: case FLOAT: return 4;
                case DOUBLE: return 8;
                default: return 0;
            }
        }
    }

    public class DataPair {
        public DataType type;
        public int count;

        public DataPair(DataType type, int count) {
            this.type = type;
            this.count = count;
        }
    }
}
