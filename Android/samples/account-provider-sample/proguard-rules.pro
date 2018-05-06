# SampleAccountProviders Proguard Rules

# Set attributes to modify default Proguard behavior:
#   *Annotation*    - Prevents annotation removal
#   Signature       - Preserves generic method signature info
#   SourceFile      - Preservces source file info for stack traces
#   LineNumberTable - Preserves original line number info for stack traces
#   EnclosingMethod - Preserves info for methods in which classes are defined
#   InnerClasses    - Prevents removal of inner classes
-keepattributes *Annotation*, Signature, SourceFile, LineNumberTable, EnclosingMethod, InnerClasses

# Prevent removal of the Keep annotation
-keep @interface android.support.annotation.Keep

# Ensure Keep annotated classes and interfaces are actually kept
-keep @android.support.annotation.Keep class * {*;}
-keep @android.support.annotation.Keep interface * {*;}

# Preserve public inner interfaces (like the InnerClasses attribute, but no corresponding InnerInterfaces attribute exists)
-keep public interface **$* {*;}

# Preserve enum methods
# Don't use 'allowoptimization' option as it prevents lookup via reflection
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve inner enum class methods
# Don't use 'allowoptimization' option as it prevents lookup via reflection
-keepclassmembers enum **$* {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}