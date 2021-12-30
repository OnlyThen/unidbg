package com.github.unidbg.android;

import com.github.unidbg.AbstractEmulator;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ThreadTest extends AbstractJni {

    private final AndroidEmulator emulator;

    private ThreadTest() {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .addBackendFactory(new DynarmicFactory(true))
                .setProcessName("test").build();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        VM vm = emulator.createDalvikVM();
        vm.setJni(this);
        vm.setVerbose(true);

        emulator.getSyscallHandler().setVerbose(true);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);

        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/armeabi-v7a/libthread-lib.so"), false);
        Pointer ptr = UnidbgPointer.pointer(emulator, dm.getModule().base + 0x85a0);
        Objects.requireNonNull(ptr).setShort(0, (short) 0xdffe); // svc #0xfe 线程调度中断

        dm.callJNI_OnLoad(emulator);
        DvmClass cMainActivity = vm.resolveClass("com/mpt/jnithread/MainActivity");
        DvmObject<?> ret = cMainActivity.callStaticJniMethodObject(emulator, "stringFromJNI()Ljava/lang/String;");
        System.err.println("ret=" + ret);
    }

    private void destroy() throws IOException {
        emulator.close();
        System.out.println("destroy");
    }

    public static void main(String[] args) throws Exception {
        Logger.getLogger(ARM32SyscallHandler.class).setLevel(Level.INFO);
        Logger.getLogger(AbstractEmulator.class).setLevel(Level.INFO);
        ThreadTest test = new ThreadTest();
        test.destroy();
    }

}
