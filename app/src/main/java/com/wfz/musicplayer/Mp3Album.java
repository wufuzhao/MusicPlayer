package com.wfz.musicplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Wufuzhao on 2017/4/13.
 */

public class Mp3Album {
    public class ID3V2Header {
        public byte[] header = new byte[3]; /* 字符串 "ID3" */
        public byte version;     /* 版本号ID3V2.3 就记录3 */
        public byte reVersion;  /* 副版本号此版本记录为0 */
        public byte flag;   /* 存放标志的字节，这个版本只定义了三位，很少用到，可以忽略 */
        public byte[] size = new byte[4];/* 大小，除了标签头的10 个字节的标签帧的大小
        大小为四个字节，但每个字节只用低7位，最高位不使用，恒为0，其格式如下：
                0xxxxxxx 0xxxxxxx 0xxxxxxx 0xxxxxxx*/
        public static final int BYTE_COUNT = 10;

        public ID3V2Header(InputStream inputStream) throws Exception {
            byte[] data = new byte[BYTE_COUNT];
            int i = inputStream.read(data);
            if (i != -1)
                init(data);
            else
                throw new Exception("the end of the stream has been reached");
        }

        public void init(byte[] data) {
            header[0] = data[0];
            header[1] = data[1];
            header[2] = data[2];
            version = data[3];
            reVersion = data[4];
            flag = data[5];
            size[0] = data[6];
            size[1] = data[7];
            size[2] = data[8];
            size[3] = data[9];
        }

        public int getTagSize() {
            return (size[0] & 0x7F) * 0x200000
                    + (size[1] & 0x7F) * 0x4000
                    + (size[2] & 0x7F) * 0x80
                    + (size[3] & 0x7F);
        }
    }

    public class ID3V2Frame {
        public byte[] frameID = new byte[4];
        public int[] size = new int[4]; /*4个字节的长度，这次每个字节都用全8位，0-255，java的byte是-128-127*/
        public byte[] flag = new byte[2];
        public static final int BYTE_COUNT = 10;

        public ID3V2Frame(InputStream inputStream) throws Exception {
            byte[] data = new byte[BYTE_COUNT];
            int i = inputStream.read(data);
            if (i != -1)
                init(data);
            else
                throw new Exception("the end of the stream has been reached");
        }

        public void init(byte[] data) {
            frameID[0] = data[0];
            frameID[1] = data[1];
            frameID[2] = data[2];
            frameID[3] = data[3];
            size[0] = data[4] < 0 ? 256 + data[4] : data[4];
            size[1] = data[5] < 0 ? 256 + data[5] : data[5];
            size[2] = data[6] < 0 ? 256 + data[6] : data[6];
            size[3] = data[7] < 0 ? 256 + data[7] : data[7];
            flag[0] = data[8];
            flag[1] = data[9];
        }

        public int getFrameSize() {
            return size[0] << 24 | size[1] << 16 | size[2] << 8 | size[3];
        }
    }

    public Bitmap getMp3Album(InputStream inputStream) {
        try {
            ID3V2Header id3V2Header = new ID3V2Header(inputStream);
            String header = new String(id3V2Header.header);
            Log.d("Mp3Album", "ID3V2Header.header = " + header);
            if ("ID3".equals(header) && id3V2Header.version == 3) {
                int readed = ID3V2Header.BYTE_COUNT;
                int tagSize = id3V2Header.getTagSize();
                while (true) {
                    //超出范围还没读到"APIC"
                    if ((readed - ID3V2Header.BYTE_COUNT) >= tagSize)
                        break;
                    ID3V2Frame frame = new ID3V2Frame(inputStream);
                    readed += ID3V2Frame.BYTE_COUNT;
                    String frameID = new String(frame.frameID);
                    Log.d("Mp3Album", "ID3V2Frame.frameID = " + frameID);
                    int frameSize = frame.getFrameSize();
                    Log.d("Mp3Album", "ID3V2Frame.FrameSize = " + frameSize);
                    if ("APIC".equals(frameID)) {
                        /*图片格式前一位是0的话，图片数据一般跟图片格式隔3字节,
                        * 1的话一般后面还有一段描述，不过也有例外的，没搞懂，所以直接找jpg和png的开头匹配*/
                        int flag = inputStream.read();
                        readed = 1;
                        //后面是图片格式
                        StringBuilder sb = new StringBuilder();
                        int c;
                        while ((c = (byte) inputStream.read()) != 0) {
                            sb.append((char) c);
                            readed++;
                        }
                        readed++;   //while最后一次
                        Log.d("Mp3Album", "图片格式：" + sb.toString());
                        byte[] buf = new byte[frameSize - readed];
                        if (inputStream.read(buf) == -1)
                            break;
                        int offset = getImageDataStart(buf);
                        if (offset == -1)
                            return null;
                        Bitmap bm = BitmapFactory.decodeByteArray(buf, offset, buf.length - offset);
                        Log.d("Mp3Album", "Bitmap大小:" + bm.getByteCount());
                        inputStream.close();
                        return bm;
                    } else {
                        //跳到下一帧
                        inputStream.skip(frameSize);
                        readed += frameSize;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final int[] JPG_HEARD = new int[]{0xFF, 0xD8, 0xFF};
    public static final int[] PNG_HEARD = new int[]{0x89, 0x50, 0x4E};

    private int getImageDataStart(byte[] buf) {
        int l = buf.length - 2;
        for (int i = 0; i < l; i++) {
            if (buf[i] == (byte) JPG_HEARD[0]) {
                if (buf[i + 1] == (byte) JPG_HEARD[1]) {
                    if (buf[i + 2] == (byte) JPG_HEARD[2]) {
                        return i;
                    }
                }
            } else if (buf[i] == (byte) PNG_HEARD[0]) {
                if (buf[i + 1] == (byte) PNG_HEARD[1]) {
                    if (buf[i + 2] == (byte) PNG_HEARD[2]) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
}
