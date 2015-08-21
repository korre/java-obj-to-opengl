# java-obj-to-opengl
Java class that reads and converts a wavefront .obj 3d model file to java object.
The result object will contain all the necessary data for render of the 3d model with OpenGL ES on for example Android.

Resulting file will contain float buffers with Vertices, Normals and Texture Coordinates.

### How to use
```
MeshObjectLoader.loadModelMeshFromStream(InputStream is)
```

Where parameter 'is' is a .obj file.

