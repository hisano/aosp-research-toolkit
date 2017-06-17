package jp.hisano.aosp_research_toolkit;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.request.MethodEntryRequest;

final class DebuggerUtils {
	private static final int ANDROID_DEFAULT_DEBUGGEE_PORT = 8700;

	static VirtualMachine attachToDebuggee() {
		return attachToDebuggee(ANDROID_DEFAULT_DEBUGGEE_PORT);
	}

	static VirtualMachine attachToDebuggee(int port) {
		AttachingConnector connector = (AttachingConnector) Bootstrap.virtualMachineManager().allConnectors().stream().filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findFirst().get();
		Map<String, Connector.Argument> arguments = connector.defaultArguments();
		arguments.get("hostname").setValue("localhost");
		arguments.get("port").setValue("" + port);
		try {
			return connector.attach(arguments);
		} catch (IOException | IllegalConnectorArgumentsException e) {
			throw new DebuggerException(e);
		}
	}

	static void addClassFilterForSystemServices(VirtualMachine vm) {
		addClassFilters(vm, "com.android.server.*");
	}

	static void addClassFilters(VirtualMachine vm, String... classFilterPatterns) {
		MethodEntryRequest request = vm.eventRequestManager().createMethodEntryRequest();
		request.setSuspendPolicy(MethodEntryRequest.SUSPEND_EVENT_THREAD);
		Stream.of(classFilterPatterns).forEach(request::addClassFilter);
		request.enable();
	}

	private DebuggerUtils() {
	}
}
