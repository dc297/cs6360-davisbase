import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BTree {

    RandomAccessFile file;
	public Element root;
    static int NODE_ARITY = 12;
	public int TREE_SIZE = 0;
    static int OFFSET = 16;
	static int order = NODE_ARITY;
	static int NODE_POINTER_EMPTY = 8;
	static int KEY_LENGTH = 32;
    static int KEY_SIZE = KEY_LENGTH * NODE_ARITY;
    static int VALUE_LENGTH = 293;
    static int VALUE_SIZE = VALUE_LENGTH * NODE_ARITY;
    static int PARENT_SIZE = 8;
    static int NODE_SIZE = 4096;
    static int NUM_ELEMENTS_SIZE = 4;
    static int PAD = NODE_SIZE - PARENT_SIZE - NUM_ELEMENTS_SIZE - KEY_SIZE - VALUE_SIZE;
    static final boolean DEBUG = false;
    public LinkedList<Long> emptyNodes = new LinkedList<Long>();
    public Map<String, Element> nodeCache;
    public boolean loading = false;
    int removed = 0;

	public BTree(RandomAccessFile file) {
		try {
			this.file = file;
			if (file.length() == 0) {
				file.seek(0);
				file.writeInt(TREE_SIZE);
				file.writeInt(NODE_ARITY);
				file.writeLong(-1);
			} else {
				root = new Element();
				root = root.read(OFFSET);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		Map modLinkHashMap = new LinkedHashMap<String, Element>(200) {

			private static final int ENTRIES = 1000;

			@Override
			protected boolean removeEldestEntry(Map.Entry eldest) {
				return size() > ENTRIES;
			}
		};
		nodeCache = modLinkHashMap;
	}

    public boolean add(String key, String data) {
		boolean isNativeAdd = false;
		try {
			if (key != null) {
				if (file.length() <= OFFSET) {
					root = new Element(key, data);
					root.parent = -1;
					file.seek(OFFSET);
					root.commit(root, -1);
					isNativeAdd = true;
				} else {
					boolean needSplit = root.set(key, data);
					if (!needSplit) {
						file.seek(OFFSET);
						root.commit(root, -1);
						isNativeAdd = true;
					} else {
						root.commit(root, -1);
						root.splitRoot();
						root.commit(root, -1);
						isNativeAdd = true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (isNativeAdd) {
			this.TREE_SIZE++;
			return true;
		} else {
			return false;
		}
    }

    private class Element {

        List<String> values;
    	List<String> keys;
        long parent;

        public Element(String key, String value) {
            this.values = new LinkedList();
        	this.keys = new LinkedList();
            this.values.add(value);
        	this.keys.add(key);
        }

        public Element() {
            this.parent = -1;
            this.values = new LinkedList();
            this.keys = new LinkedList();
        }


        public long popEmptyNodePointer() throws Exception {
			file.seek(NODE_POINTER_EMPTY);
			long emptyPointer = file.readLong();

			if (emptyPointer != -1) {
				Element topNode = this.fetchElement(emptyPointer);
				if (!topNode.values.get(0).equals("$END")) {
					String nodePointer = topNode.values.get(0);
					long nextEmptyNode = this.getPointerLocation(nodePointer);
					file.seek(NODE_POINTER_EMPTY);
					file.writeLong(nextEmptyNode);
				} else {
					file.seek(NODE_POINTER_EMPTY);
					file.writeLong(-1);
				}
				return emptyPointer;
			} else {
				return -1;
			}
        }

        public void commit(Element node, long seek) {
            try {
                if (node.parent == -1 || seek == -1) {
                    file.seek(OFFSET);
                    file.writeLong(-1);
                } else {
                    file.seek(seek);
                    file.writeLong(node.parent);
                }
                int numElements = node.keys.size();
                file.writeInt(numElements);
                for (String s : node.keys) {
                    byte[] bytes = s.getBytes();
                    file.write(bytes);
                    int tmpPad = KEY_LENGTH - bytes.length;
                    byte[] buffer1 = new byte[tmpPad];
                    for (int i = 0; i < tmpPad; i++) {
                        buffer1[i] = ' ';
                    }
                    file.write(buffer1);
                }
                int padNumBytes = (NODE_ARITY - numElements) * KEY_LENGTH;
                byte[] buffer2 = new byte[padNumBytes];
                for (int i = 0; i < padNumBytes; i++) {
                    buffer2[i] = ' ';
                }
                file.write(buffer2);
                for (String s : node.values) {
                    byte[] bytes = s.getBytes();
                    file.write(bytes);
                    int tmpPad = VALUE_LENGTH - bytes.length;
                    byte[] buffer3 = new byte[tmpPad];
                    for (int i = 0; i < tmpPad; i++) {
                        buffer3[i] = ' ';
                    }
                    file.write(buffer3);
                }
                int padNumBytes2 = (NODE_ARITY - numElements) * VALUE_LENGTH;
                byte[] buffer4 = new byte[padNumBytes2];
                for (int i = 0; i < padNumBytes2; i++) {
                    buffer4[i] = ' ';
                }
                file.write(buffer4);

                byte[] buffer5 = new byte[PAD];
                for (int i = 0; i < PAD; i++) {
                    buffer5[i] = ' ';
                }
                file.write(buffer5);
                nodeCache.put(seek + "", node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Element read(long seek) {
            try {
                Element tmp = new Element();
                file.seek(seek);
                tmp.parent = file.readLong();
                int numElements = file.readInt();
                long pointer = file.getFilePointer();
                for (int i = 0; i < numElements; i++) {
                    byte[] inBytes = new byte[KEY_LENGTH];
                    file.readFully(inBytes, 0, KEY_LENGTH);
                    String s = new String(inBytes);
                    s = s.trim();
                    tmp.keys.add(s);
                }
                long valuePointer = pointer + KEY_SIZE;
                file.seek(valuePointer);
                for (int i = 0; i < numElements; i++) {
                    byte[] inBytes = new byte[VALUE_LENGTH];
                    file.readFully(inBytes, 0, VALUE_LENGTH);
                    String s = new String(inBytes);
                    s = s.trim();
                    tmp.values.add(s);
                }
                return tmp;
            } catch (Exception e) {
                System.out.println("Error seek is: " + seek);
                e.printStackTrace();
                return null;
            }
        }

        public Element fetchElement(long address) {
            Element n;
            if (nodeCache.containsKey(address + "")) {
                n = nodeCache.get(address + "");
            } else {
                n = this.read(address);
            }
            return n;
        }

        public boolean hasChildren() {
            if (this.values.size() == 0) {
                return false;
            } else {
                if (this.values.get(0).startsWith("$")) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        public void splitRoot() {
            boolean hadChildren = hasChildren();

            boolean fix = true;
            Element left = new Element();
            Element right = new Element();


            long leftPointer = this.getNextAvailPointer();
            nodeCache.put(leftPointer + "", left);
            long rightPointer = this.getNextAvailPointer();
            nodeCache.put(rightPointer + "", right);
            int half = (int) Math.ceil(1.0 * BTree.order / 2);
            int size = this.keys.size();
            for (int i = 0; i < size; i++) {
                if (i < half) {
                    left.set(this.keys.remove(0), this.values.remove(0));
                } else {
                    right.set(this.keys.remove(0), this.values.remove(0));
                }
            }
            if (hadChildren) {
                fix = false;
                this.set(left.keys.set(left.keys.size() - 1, "null"), this.createPointerLocation(leftPointer));
            }
            left.parent = OFFSET;
            right.parent = OFFSET;
            if (fix) {
                this.set(right.keys.get(0), this.createPointerLocation(leftPointer));
            }
            this.set("null", this.createPointerLocation(rightPointer));
            this.commit(left, leftPointer);
            this.commit(right, rightPointer);
        }

        private String createPointerLocation(long pointer) {
            return "$" + pointer;
        }

        private long getPointerLocation(String pointer) {
            if (pointer.startsWith("$")) {
                return Long.parseLong(pointer.substring(1));
            } else {
                return -99;
            }
        }

        private long getNextAvailPointer() {
            try {
                long nextPointer = this.popEmptyNodePointer();
                if (nextPointer == -1) {
                    long length = file.length();
                    if (!nodeCache.containsKey(length + "")) {
                        return length;
                    } else {
                        while (nodeCache.containsKey(length + "")) {
                            length = length + NODE_SIZE;
                        }
                    }
                    return length;
                } else {
                    return nextPointer;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        private void splitLeafNode(Element right) {
            Element left = new Element();
            long leftPointer = this.getNextAvailPointer();
            nodeCache.put(leftPointer + "", left);
            int half = (int) Math.ceil(1.0 * BTree.order / 2);
            for (int i = 0; i < half; i++) {
                left.set(right.keys.remove(0), right.values.remove(0));
            }
            this.set(right.keys.get(0), this.createPointerLocation(leftPointer));
            this.commit(left, leftPointer);
        }

        private void splitInternalNode(Element right) {
            Element left = new Element();
            long leftPointer = this.getNextAvailPointer();
            nodeCache.put(leftPointer + "", left);
            int half = (int) Math.ceil(1.0 * BTree.order / 2);
            for (int i = 0; i < half; i++) {
                left.set(right.keys.remove(0), right.values.remove(0));
            }
            String s = left.keys.set(left.keys.size() - 1, "null");
            this.set(s, this.createPointerLocation(leftPointer));
            this.commit(left, leftPointer);
        }

        private boolean needToSplit() {
            return this.keys.size() > BTree.order - 1;
        }

        private void splitChild(Element n) {
            if (!n.hasChildren()) {
                this.splitLeafNode(n);
            } else {
                this.splitInternalNode(n);
            }
        }
 
        public boolean set(String key, String value) {
            if (this.keys.size() == 0) {
                this.keys.add(key);
                this.values.add(value);
                return this.needToSplit();
            } else {
                if (!this.hasChildren()) {
                    int ks = this.keys.size();
                    for (int i = 0; i < ks; i++) {
                        if (key.equals("null")) {
                            this.keys.add(key);
                            this.values.add(value);
                            return this.needToSplit();
                        }
                        int cp = this.keys.get(i).compareTo(key);
                        
                        if (key.equals(this.keys.get(i))) {
                            this.keys.set(i, key);
                            this.values.set(i, value);
                            return this.needToSplit();
                        }

                        if (cp < 0) {
                            this.keys.add(i, key);
                            this.values.add(i, value);
                            return this.needToSplit();
                        }
                    }

                    this.keys.add(key);
                    this.values.add(value);
                    return this.needToSplit();
                } else {
                    if (value.startsWith("$")) {
                        if (key.equals("null")) {
                            if (this.keys.get(this.keys.size() - 1).equals("null")) {
                                this.keys.set(this.keys.size() - 1, "null");
                                this.values.set(this.values.size() - 1, value);
                                return this.needToSplit();
                            } else {
                                this.keys.add("null");
                                this.values.add(value);
                                return this.needToSplit();
                            }
                        }
                        int ks = this.keys.size();
                        for (int i = 0; i < ks; i++) {
                            if (!this.keys.get(i).equals("null")) {
                                int cp = this.keys.get(i).compareTo(key);
                                
                                if (key.equals(this.keys.get(i))) {
                                    this.keys.set(i, key);
                                    this.values.set(i, value);
                                    return this.needToSplit();
                                }
                                if (cp < 0) {
                                    this.keys.add(i, key);
                                    this.values.add(i, value);
                                    return this.needToSplit();
                                }
                            }
                        }
                        int ksize = this.keys.size();
                        for (int i = 0; i < ksize; i++) {
                            if (this.keys.get(i).equals("null")) {
                                this.keys.add(this.keys.size() - 1, key);
                                this.values.add(this.values.size() - 1, value);
                                return this.needToSplit();
                            }
                        }
                        this.keys.add(key);
                        this.values.add(value);
                        return this.needToSplit();

                    } else {
                        int ks = this.keys.size();
                        for (int i = 0; i < ks - 1; i++) {
                            int cp = this.keys.get(i).compareTo(key);
                            
                            if (key.equals(this.keys.get(i))) {
                                long nPointerLocation = this.getPointerLocation(this.values.get(i + 1));
                                Element n = this.fetchElement(nPointerLocation);
                                if (n.set(key, value)) {
                                    this.splitChild(n);
                                    this.commit(n, nPointerLocation);
                                    return this.needToSplit();
                                } else {
                                    this.commit(n, nPointerLocation);
                                    return this.needToSplit();
                                }
                            }
                            if (cp < 0) {
                                long nPointerLocation = this.getPointerLocation(this.values.get(i));
                                Element n = this.fetchElement(nPointerLocation);
                                if (n.set(key, value)) {
                                    this.splitChild(n);
                                    this.commit(n, nPointerLocation);
                                    return this.needToSplit();
                                } else {
                                    this.commit(n, nPointerLocation);
                                    return this.needToSplit();
                                }
                            }
                        }

                        long nPointerLocation = this.getPointerLocation(this.values.get(this.keys.size() - 1));
                        Element n = this.fetchElement(nPointerLocation);
                        if (n.set(key, value)) {
                            this.splitChild(n);
                            this.commit(n, nPointerLocation);
                            return this.needToSplit();
                        } else {
                            this.commit(n, nPointerLocation);
                            return this.needToSplit();
                        }
                    }
                }
            }
        }
    }
}
