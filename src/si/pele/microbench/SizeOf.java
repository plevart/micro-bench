package si.pele.microbench;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;

public class SizeOf
{
    private static Instrumentation instrumentation;

    /**
     * Initializes agent
     */
    public static void premain(String agentArgs,
                               Instrumentation instrumentation)
    {
        SizeOf.instrumentation = instrumentation;
    }

    private final Visitor visitor;
    private final Object[] internedObjects;

    public SizeOf()
    {
        this(Visitor.NULL);
    }

    public SizeOf(Visitor visitor, Object ... internedObjects)
    {
        this.visitor = visitor;
        this.internedObjects = internedObjects;
    }

    /**
     * Returns object size.
     */
    public long sizeOf(Object obj)
    {
        if (instrumentation == null)
        {
            throw new IllegalStateException(
                "Instrumentation environment not initialised.");
        }

        return instrumentation.getObjectSize(obj);
    }

    /**
     * Returns deep size of object, recursively iterating over
     * its fields and superclasses.
     */
    public long deepSizeOf(Object obj)
    {
        return visitObject(obj, new IdentityHashMap(), 0);
    }

    /**
     * Returns true if this is a well-known shared flyweight.
     * For example, interned Strings, Booleans and Number objects or Class instances
     */
    private boolean isInterned(Object obj)
    {
        if (obj instanceof Comparable)
        {
            if (obj instanceof Enum)
            {
                return true;
            }
            else if (obj instanceof String)
            {
                return (obj == ((String) obj).intern());
            }
            else if (obj instanceof Boolean)
            {
                return (obj == Boolean.TRUE || obj == Boolean.FALSE);
            }
            else if (obj instanceof Integer)
            {
                return (obj == Integer.valueOf((Integer) obj));
            }
            else if (obj instanceof Short)
            {
                return (obj == Short.valueOf((Short) obj));
            }
            else if (obj instanceof Byte)
            {
                return (obj == Byte.valueOf((Byte) obj));
            }
            else if (obj instanceof Long)
            {
                return (obj == Long.valueOf((Long) obj));
            }
            else if (obj instanceof Character)
            {
                return (obj == Character.valueOf((Character) obj));
            }
        }
        else if (obj instanceof Class || obj instanceof ClassLoader)
        {
            return true;
        }
        else
        {
            for (Object o : internedObjects)
            {
                if (o == obj)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private long visitObject(Object obj, Map visited, int level)
    {
        long bytes;
        Class clazz;
        if (obj == null || visited.put(obj, Boolean.TRUE) != null || (level > 0 && isInterned(obj)))
        {
            bytes = 0L;
            visitor.startObject(obj, level, bytes);
            visitor.endObject(obj, level, bytes);
        }
        else if ((clazz = obj.getClass()).isArray() && !clazz.getComponentType().isPrimitive())
        {
            bytes = sizeOf(obj);
            visitor.startObject(obj, level, bytes);
            for (Object e : (Object[]) obj)
            {
                bytes += visitObject(e, visited, level + 1);
            }
            visitor.endObject(obj, level, bytes);
            return bytes;
        }
        else
        {
            bytes = sizeOf(obj);
            visitor.startObject(obj, level, bytes);
            bytes += visitFields(obj, visited, level + 1);
            visitor.endObject(obj, level, bytes);
        }

        return bytes;
    }

    private long visitFields(Object obj, Map visited, int level)
    {
        long bytes = 0L;
        LinkedList<Class<?>> classes = new LinkedList<>();
        for (Class<?> clazz = obj.getClass(); clazz != null; clazz = clazz.getSuperclass())
            classes.addFirst(clazz);
        for (Class<?> clazz : classes)
        {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields)
            {
                if (!Modifier.isStatic(field.getModifiers()))
                {
                    Class<?> type = field.getType();
                    String name = field.getName();
                    visitor.startField(name, type, level);
                    if (!type.isPrimitive())
                    {
                        try
                        {
                            field.setAccessible(true);
                            bytes += visitObject(field.get(obj), visited, level);
                        }
                        catch (IllegalAccessException ex)
                        {
                            throw new RuntimeException(ex);
                        }
                    }
                    visitor.endField(name, type, level);
                }
            }
        }

        return bytes;
    }

    public interface Visitor
    {
        Visitor NULL = new Visitor()
        {
            @Override
            public void startObject(Object o, int level, long shallowBytes)
            {
            }

            @Override
            public void endObject(Object o, int level, long deepBytes)
            {
            }

            @Override
            public void startField(String name, Class<?> type, int level)
            {
            }

            @Override
            public void endField(String name, Class<?> type, int level)
            {
            }
        };

        Visitor STDOUT = new Visitor()
        {
            boolean inField;
            boolean inObject;

            private String indent(int level)
            {
                StringBuilder sb = new StringBuilder(level * 2);
                for (int i = 0; i < level; i++)
                    sb.append("  ");
                return sb.toString();
            }

            private String typeName(Class<?> clazz)
            {
                if (clazz.isArray())
                    return typeName(clazz.getComponentType()) + "[]";
                else
                    return clazz.getName();
            }

            @Override
            public void startObject(Object o, int level, long shallowBytes)
            {
                if (inObject) System.out.println(" {");

                System.out.print(inField ? ": " : indent(level));

                if (o == null)
                {
                    System.out.print("null");
                }
                else
                {
                    Class<?> clazz = o.getClass();
                    if (clazz.isArray())
                    {
                        System.out.print(typeName(clazz.getComponentType()) +
                                             "[" + Array.getLength(o) +
                                             "]@" + Integer.toHexString(System.identityHashCode(o)));
                    }
                    else
                    {
                        System.out.print(typeName(clazz) +
                                             "@" + Integer.toHexString(System.identityHashCode(o)));
                    }

                    if (shallowBytes == 0)
                        System.out.print("(interned)");
                    else
                        System.out.print("(" + shallowBytes + " bytes)");
                }

                inField = false;
                inObject = true;
            }

            @Override
            public void endObject(Object o, int level, long deepBytes)
            {
                if (inObject || o == null)
                {
                    System.out.println();
                    inObject = false;
                }
                else
                {
                    System.out.println(indent(level) + "}->(" + deepBytes + " deep bytes)");
                }
            }

            @Override
            public void startField(String name, Class<?> type, int level)
            {
                if (inObject) System.out.println(" {");
                System.out.print(indent(level) + name);
                inField = true;
                inObject = false;
            }

            @Override
            public void endField(String name, Class<?> type, int level)
            {
                if (inField)
                {
                    System.out.println(": " + typeName(type));
                    inField = false;
                }
            }
        };

        void startObject(Object o, int level, long shallowBytes);

        void endObject(Object o, int level, long deepBytes);

        void startField(String name, Class<?> type, int level);

        void endField(String name, Class<?> type, int level);
    }
}
