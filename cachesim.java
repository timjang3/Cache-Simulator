import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;

public class cachesim {
    public static class Cache {
        int numFrames, numSets, blockSize;
        String writeMethod;
        Data[][] cache;
        LinkedList<Integer>[] lru;
        MainMemory memory;

        public Cache(int cacheSize, int waySize, String writeMethod, int blockSize, MainMemory memory){
            this.writeMethod = writeMethod;
            numFrames = (1024 * cacheSize) / blockSize;
            numSets = numFrames / waySize;
            cache = new Data[numSets][waySize];
            this.memory = memory;
            this.blockSize = blockSize;
            lru = new LinkedList[numSets];

            for(int i = 0; i < lru.length; i++){
                LinkedList list = new LinkedList<Integer>();
                lru[i] = list;
            }

            for(int way = 0; way < cache[0].length; way++){
                for(int set = 0; set < cache.length; set++ ){
                    Data d = new Data(blockSize);
                    cache[set][way] = d;
                }
            }

        }

        public String[] read(int address, int bytes){
            int tag = address / (numSets*blockSize);
            int index = ( address / blockSize ) % numSets;
            int offset = address % blockSize;
            long blockData;


            for(int way = 0; way < cache[0].length; way++){
                if(cache[index][way].isValid() == true){
                    if(cache[index][way].getTag() == tag){
                        blockData = cache[index][way].getData(offset, bytes);
                        LinkedList list = lru[index];
                        for(int i = 0; i < list.size(); i++){
                            if(list.get(i) == (Integer) way){
                                list.remove(i);
                                break;
                            }
                        }
                        list.add(way);
                        lru[index] = list;

                        String ans = Long.toHexString(blockData);
                        int length = ans.length();
                        for(int i = 0; i < bytes*2 - length; i++){
                            ans = "0" + ans;
                        }
                        String[] temp = {ans, "1"};
                        return temp;
                    }
                }
            }


            for(int way = 0; way < cache[0].length; way++){
                if(cache[index][way].isValid() == false){
                    cache[index][way].setValid(true);
                    int startIndex = ((int) (address / blockSize)) * blockSize;
                    cache[index][way].setData(startIndex, startIndex + blockSize, memory, tag);
                    LinkedList<Integer> list = lru[index];
                    list.add(way);
                    lru[index] = list;
                    blockData = cache[index][way].getData(offset, bytes);
                    String ans = Long.toHexString(blockData);
                    int length = ans.length();
                    for(int i = 0; i < bytes*2 - length; i++){
                        ans = "0" + ans;
                    }
                    String[] temp = {ans, "0"};
                    return temp;
                }
            }

            int way = lru[index].removeFirst();

            if(cache[index][way].isDirty() == true){
                int start = cache[index][way].getTag() * blockSize * numSets + index * blockSize;
                memory.setBlock(start, start + blockSize, cache[index][way].getBlockData());
            }

            cache[index][way].setValid(true);
            int startIndex = ((int) (address / blockSize)) * blockSize;
            cache[index][way].setData(startIndex, startIndex + blockSize, memory, tag);
            cache[index][way].setNotDirty();
            LinkedList<Integer> list = lru[index];
            list.add(way);
            lru[index] = list;
            blockData = cache[index][way].getData(offset, bytes);
            String ans = Long.toHexString(blockData);
            int length = ans.length();
            for(int i = 0; i < bytes*2 - length; i++){
                ans = "0" + ans;
            }
            String[] temp = {ans, "0"};

            return temp;
        }

        public int write(int address, int size, BigInteger replace){
            long replacement = replace.longValue();
            int tag = address / (numSets*blockSize);
            int index = ( address / blockSize ) % numSets;
            int offset = address % blockSize;

            for(int way = 0; way < cache[0].length; way++){
                if(cache[index][way].isValid() == true) {
                    if(cache[index][way].getTag() == tag){
                        cache[index][way].changeData(offset, size, replacement);
                        cache[index][way].setDirty();
                        LinkedList list = lru[index];
                        for(int i = 0; i < list.size(); i++){
                            if(list.get(i) == (Integer) way){
                                list.remove(i);
                                break;
                            }
                        }
                        list.add(way);
                        lru[index] = list;
                        return 1;
                    }
                }
            }

            for(int way = 0; way < cache[0].length; way++){
                if(cache[index][way].isValid() == false){
                    cache[index][way].setValid(true);
                    int startIndex = ((int) (address / blockSize)) * blockSize;
                    cache[index][way].setData(startIndex, startIndex + blockSize, memory, tag);
                    cache[index][way].changeData(offset, size, replacement);
                    cache[index][way].setDirty();
                    LinkedList<Integer> list = lru[index];
                    list.add(way);
                    lru[index] = list;
                    return 0;
                }
            }

            int way = lru[index].removeFirst();

            LinkedList<Integer> list = lru[index];
            list.add(way);
            lru[index] = list;

            if(cache[index][way].isDirty() == true){
                int start = cache[index][way].getTag() * blockSize * numSets + index * blockSize;
                memory.setBlock(start, start + blockSize, cache[index][way].getBlockData());
            }

            cache[index][way].setValid(true);
            int startIndex = ((int) (address / blockSize)) * blockSize;
            cache[index][way].setData(startIndex, startIndex + blockSize, memory, tag);
            cache[index][way].changeData(offset, size, replacement);
            cache[index][way].setDirty();

            return 0;
        }

        public int writeWT(int address, int size, BigInteger replace){
            long replacement = replace.longValue();
            int tag = address / (numSets*blockSize);
            int index = ( address / blockSize ) % numSets;
            int offset = address % blockSize;

            for(int way = 0; way < cache[0].length; way++){
                if(cache[index][way].isValid() == true) {
                    if(cache[index][way].getTag() == tag){
                        cache[index][way].changeData(offset, size, replacement);
                        LinkedList list = lru[index];
                        for(int i = 0; i < list.size(); i++){
                            if(list.get(i) == (Integer) way){
                                list.remove(i);
                                break;
                            }
                        }
                        list.add(way);
                        lru[index] = list;
                        int start = tag * blockSize * numSets + index * blockSize;
                        memory.setBlock(start, start + blockSize, cache[index][way].getBlockData());
                        return 1;
                    }
                }
            }
            /*
            for(int way = 0; way < cache[0].length; way++){
                if(cache[index][way].isValid() == false){
                    cache[index][way].setValid(true);
                    int startIndex = ((int) (address / blockSize)) * blockSize;
                    cache[index][way].setData(startIndex, startIndex + blockSize, memory, tag);
                    cache[index][way].changeData(offset, size, replacement);
                    int start = tag * blockSize * numSets + index * blockSize;
                    memory.setBlock(start, start + blockSize, cache[index][way].getBlockData());
                    LinkedList<Integer> list = lru[index];
                    list.add(way);
                    lru[index] = list;
                    return 0;
                }
            }
            */

            int start = tag * blockSize * numSets + index * blockSize;
            int[] block = memory.getBlock(start, start + blockSize);

            String bitString = Long.toBinaryString(replacement);
            int length = bitString.length();
            for(int i = 0 ; i < size*8 - length; i ++){
                bitString = "0" + bitString;
            }
            int count = 0;
            for(int i = offset; i < offset + size; i++){
                block[i] = Integer.parseInt(bitString.substring(count, count+8), 2);
                count += 8;
            }

            memory.setBlock(start, start + blockSize, block);

            return 0;
        }

    }

    public static class Data{
        int tag;
        boolean valid, dirty;
        int[] blockData;

        public Data(int blockSize){
            blockData = new int[blockSize];
            tag = 0;
            valid = false;
            dirty = false;
            for(int i = 0 ; i < blockData.length; i++){
                blockData[i] = 0;
            }
        }

        public int getTag(){
            return tag;
        }

        public int[] getBlockData(){
            return blockData;
        }

        public long getData(int offset, int size){
            String bitString = "";
            for(int i = 0 ; i < size; i++){
                bitString += String.format("%8s", Integer.toBinaryString(blockData[offset + i])).replace(' ', '0');
            }
            return Long.parseLong(bitString, 2);
        }

        public boolean isValid(){
            return valid;
        }

        public boolean isDirty(){ return dirty; }

        public void setDirty(){
            dirty = true;
        }

        public void setNotDirty(){
            dirty = false;
        }

        public void setValid(boolean value){
            valid = value;
        }

        public void setData(int start, int end, MainMemory memory, int tagNum){
            blockData = memory.getBlock(start, end);
            tag = tagNum;
        }

        public void changeData(int offset, int size, long replacement){
            String bitString = Long.toBinaryString(replacement);
            int length = bitString.length();
            for(int i = 0 ; i < size*8 - length; i ++){
                bitString = "0" + bitString;
            }
            int start =0;
            for(int i = offset; i < offset + size; i++){
                blockData[i] = Integer.parseInt(bitString.substring(start, start+8), 2);
                start += 8;
            }
        }
    }

    public static class MainMemory {
        int[] data;
        int memorySize = 65536;

        public MainMemory() {
            data = new int[memorySize];
            for (int i = 0; i < data.length; i++) {
                data[i] = 0;
            }
        }

        public int getByte(int address) {
            return data[address];
        }

        public int[] getBlock(int start, int end) {
            int[] temp = new int[end - start];
            int index = 0;
            for (int i = start; i < end; i++) {
                temp[index] = data[i];
                index++;
            }
            return temp;
        }

        public void setBlock(int start, int end, int[] newData){
            int count = 0;
            for(int i = start; i < end; i++){
                data[i] = newData[count];
                count++;
            }
        }
    }

    public static void main(String args[]) throws FileNotFoundException {
        String filename = args[0];
        int cacheSize = Integer.parseInt(args[1]);
        int waySize = Integer.parseInt(args[2]);
        String writeMethod = args[3];
        int blockSize = Integer.parseInt(args[4]);


        File file = new File(filename);
        Scanner scan = new Scanner(file);

        MainMemory memory = new MainMemory();

        Cache cache = new Cache(cacheSize, waySize, writeMethod, blockSize, memory);

        while (scan.hasNextLine()){
            String data = scan.nextLine();
            String[] inst = data.split(" ");
            if(inst[0].charAt(0) == 'l'){
                String[] readData = cache.read(Integer.parseInt(inst[1], 16), Integer.parseInt(inst[2]));
                if(readData[1].equals("1")){
                    System.out.println("load " + inst[1] + " hit " + readData[0]);
                }
                else{
                    System.out.println("load " + inst[1] + " miss " + readData[0]);
                }
            }
            else{
                if(writeMethod.equals("wt")){
                    int write = cache.writeWT(Integer.parseInt(inst[1], 16), Integer.parseInt(inst[2]), new BigInteger(inst[3], 16));
                    if(write == 1){
                        System.out.println("store " + inst[1] + " hit");
                    }
                    else{
                        System.out.println("store " + inst[1] + " miss");
                    }
                }
                else{
                    int write = cache.write(Integer.parseInt(inst[1], 16), Integer.parseInt(inst[2]), new BigInteger(inst[3], 16));
                    if(write == 1){
                        System.out.println("store " + inst[1] + " hit");
                    }
                    else{
                        System.out.println("store " + inst[1] + " miss");
                    }
                }
            }
        }

        System.exit(0);
    }
}