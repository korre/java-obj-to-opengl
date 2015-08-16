package se.furniturebox.vuforia.domain.misc;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Created by Kristoffer on 15-08-06.
 */
public class MeshObjectLoader {

    private static String TAG = "ObjectLoader";
    private static int BUFFER_READER_SIZE = 65536;
    private static boolean ENABLE_LOGGING = false;

    /**
     * Load 3D model mesh from inputstream.
     * A Wavefront formatted file (.obj) is expected as input and may be loaded
     * remotely or as a local file.
     * @param is input file in form of a .obj "text file".
     * @return a object containing vertices, normals and textureCoordinates.
     */
    public static MeshArrays loadModelMeshFromStream(InputStream is) {
        BufferedReader bufferedReader = null;

        try {
            // Lists to keep all data when reading file
            ArrayList<Float> vlist = new ArrayList<Float>();
            ArrayList<Float> tlist = new ArrayList<Float>();
            ArrayList<Float> nlist = new ArrayList<Float>();
            ArrayList<Face> fplist = new ArrayList<Face>();

            // Result buffers
            FloatBuffer mVertexBuffer;
            FloatBuffer mTexBuffer;
            FloatBuffer mNormBuffer;

            int numVerts = 0;
            int numTexCoords = 0;
            int numNormals = 0;
            int numFaces = 0;

            String str;
            String[] tmp;

            bufferedReader = new BufferedReader(new InputStreamReader(is), BUFFER_READER_SIZE);

            while ((str = bufferedReader.readLine()) != null) {

                // Replace double spaces. Some files may have it. Ex. files from 3ds max.
                str = str.replace("  ", " ");
                tmp = str.split(" ");

                if (tmp[0].equalsIgnoreCase("v")) {
                    for (int i = 1; i < 4; i++) {
                        vlist.add(Float.parseFloat(tmp[i]));
                    }
                    numVerts++;
                }

                if (tmp[0].equalsIgnoreCase("vn")) {
                    for (int i = 1; i < 4; i++) {
                        nlist.add(Float.parseFloat(tmp[i]));
                    }
                    numNormals++;
                }

                if (tmp[0].equalsIgnoreCase("vt")) {
                    for (int i = 1; i < 3; i++) {
                        tlist.add(Float.parseFloat(tmp[i]));
                    }
                    numTexCoords++;
                }

                if (tmp[0].equalsIgnoreCase("f")) {

                    String[] ftmp;
                    long facex;
                    int facey;
                    int facez;

                    for (int i = 1; i < 4; i++) {
                        ftmp = tmp[i].split("/");

                        facex = Integer.parseInt(ftmp[0]) - 1;
                        facey = Integer.parseInt(ftmp[1]) - 1;
                        facez = Integer.parseInt(ftmp[2]) - 1;

                        fplist.add(new Face(facex, facey, facez));
                    }

                    numFaces++;

                    if (tmp.length > 4 && !tmp[4].equals("")) {

                        for (int i = 1; i < 5; i++) {
                            ftmp = tmp[i].split("/");

                            if (i == 1 || i == 3) {
                                facex = Integer.parseInt(ftmp[0]) - 1;
                                facey = Integer.parseInt(ftmp[1]) - 1;
                                facez = Integer.parseInt(ftmp[2]) - 1;
                                fplist.add(new Face(facex, facey, facez));
                            } else if (i == 2) {
                                String[] gtmp = tmp[4].split("/");
                                facex = Integer.parseInt(gtmp[0]) - 1;
                                facey = Integer.parseInt(gtmp[1]) - 1;
                                facez = Integer.parseInt(gtmp[2]) - 1;
                                fplist.add(new Face(facex, facey, facez));
                            }
                        }

                        numFaces++;
                    }
                }
            }

            if (ENABLE_LOGGING) {
                Log.d(TAG, "Vertices: " + numVerts);
                Log.d(TAG, "Normals: " + numNormals);
                Log.d(TAG, "TextureCoords: " + numTexCoords);
                Log.d(TAG, "Faces: " + numFaces);
            }

            // Each float takes 4 bytes
            ByteBuffer vbb = ByteBuffer.allocateDirect(fplist.size() * 4 * 3);
            vbb.order(ByteOrder.LITTLE_ENDIAN);
            mVertexBuffer = vbb.asFloatBuffer();

            ByteBuffer vtbb = ByteBuffer.allocateDirect(fplist.size() * 4 * 2);
            vtbb.order(ByteOrder.LITTLE_ENDIAN);
            mTexBuffer = vtbb.asFloatBuffer();

            ByteBuffer nbb = ByteBuffer.allocateDirect(fplist.size() * 4 * 3);
            nbb.order(ByteOrder.LITTLE_ENDIAN);
            mNormBuffer = nbb.asFloatBuffer();

            for (int j = 0; j < fplist.size(); j++) {
                mVertexBuffer.put(vlist.get((int) (fplist.get(j).fx * 3)));
                mVertexBuffer.put(vlist.get((int) (fplist.get(j).fx * 3 + 1)));
                mVertexBuffer.put(vlist.get((int) (fplist.get(j).fx * 3 + 2)));

                mTexBuffer.put(tlist.get(fplist.get(j).fy * 2));
                mTexBuffer.put(tlist.get((fplist.get(j).fy * 2) + 1));

                mNormBuffer.put(nlist.get(fplist.get(j).fz * 3));
                mNormBuffer.put(nlist.get((fplist.get(j).fz * 3) + 1));
                mNormBuffer.put(nlist.get((fplist.get(j).fz * 3) + 2));
            }

            mVertexBuffer.rewind();
            mTexBuffer.rewind();
            mNormBuffer.rewind();

            return new MeshArrays(mVertexBuffer, mNormBuffer, mTexBuffer, numFaces * 3);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Local class to keep vertex, normal and texture coordinate while reading file.
     */
    private static class Face {
        public long fx;
        public int fy;
        public int fz;

        public Face(long fx, int fy, int fz) {
            this.fx = fx;
            this.fy = fy;
            this.fz = fz;
        }
    }

    /**
     * MeshArrays keeps all necessary arrays needed to be drawn with openGL.
     */
    public static class MeshArrays {

        private Buffer vertices;
        private Buffer normals;
        private Buffer texCoords;

        private int numVertices;

        public MeshArrays(Buffer vertices, Buffer normals, Buffer texCoords, int numVertices) {
            this.vertices = vertices;
            this.normals = normals;
            this.texCoords = texCoords;
            this.numVertices = numVertices;
        }

        public Buffer getVertices() {
            return vertices;
        }

        public Buffer getNormals() {
            return normals;
        }

        public Buffer getTexCoords() {
            return texCoords;
        }

        public int getNumVertices() {
            return numVertices;
        }
    }
}
