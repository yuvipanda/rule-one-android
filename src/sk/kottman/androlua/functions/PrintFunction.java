package sk.kottman.androlua.functions;

import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

public class PrintFunction extends JavaFunction {
    private final StringBuilder output;

    public PrintFunction(LuaState L, StringBuilder output) {
        super(L);
        this.output = output;
    }

    @Override
    public int execute() throws LuaException {
        for (int i = 2; i <= L.getTop(); i++) {
            int type = L.type(i);
            String stype = L.typeName(type);
            String val = null;
            if (stype.equals("userdata")) {
                Object obj = L.toJavaObject(i);
                if (obj != null)
                    val = obj.toString();
            } else if (stype.equals("boolean")) {
                val = L.toBoolean(i) ? "true" : "false";
            } else {
                val = L.toString(i);
            }
            if (val == null)
                val = stype;
            output.append(val);
            output.append("\t");
        }
        output.append("\n");
        return 0;
    }
}
