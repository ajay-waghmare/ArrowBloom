# Arrow Bloom ProGuard / R8 rules.
# Readable stack traces from release builds (line numbers survive, file names are folded).
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
