package jp.hisano.aosp_research_toolkit;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.Sets;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.request.MethodEntryRequest;

public final class ShowCalledMethodsOfSystemServices {
	public static void main(String... args) throws IOException {
		VirtualMachine vm;
		try {
			vm = attachToDebuggee();
		} catch (IOException | IllegalConnectorArgumentsException e1) {
			System.err.println("Please, select 'system_process' process in 'Devices' view to prepare 8700 debuggee port and restart.");
			System.exit(1);
			return;
		}
		prepareSystemServiceClassesFilter(vm);

		ExecutorService worker = Executors.newSingleThreadExecutor();
		worker.submit(() -> {
			Set<String> isShownMethods = Sets.newHashSet();
			EventQueue queue = vm.eventQueue();
			while (true) {
				try {
					EventSet events = queue.remove();
					events.stream().forEach(event -> {
						if (event instanceof MethodEntryEvent) {
							String methodInformation;
							{
								Method method = ((MethodEntryEvent) event).method();
								String methodName = method.declaringType().name() + "." + method.name();
								try {
									Location location = method.location();
									if (location.lineNumber() != -1) {
										methodInformation = methodName + "(" + location.sourceName() + ":" + location.lineNumber() + ")";
									} else {
										methodInformation = null;
									}
								} catch (AbsentInformationException e) {
									methodInformation = methodName;
								}
							}
							if (methodInformation != null && isShownMethods.add(methodInformation)) {
								System.out.println(methodInformation);
							}
						}
					});
					events.resume();
				} catch (InterruptedException e) {
					break;
				}
				// The following line is required because 'remove' method never throw 'InterruptedException'
				if (Thread.interrupted()) {
					break;
				}
			}
		});

		System.out.println("Recording started. Please, press key to stop.");
		System.in.read();

		worker.shutdown();
		System.exit(0);
	}

	private static void prepareSystemServiceClassesFilter(VirtualMachine vm) {
		MethodEntryRequest request = vm.eventRequestManager().createMethodEntryRequest();
		request.addClassFilter("com.android.server.*");
		request.enable();
	}

	private static VirtualMachine attachToDebuggee() throws IOException, IllegalConnectorArgumentsException {
		AttachingConnector connector = (AttachingConnector) Bootstrap.virtualMachineManager().allConnectors().stream().filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findFirst().get();
		Map<String, Connector.Argument> arguments = connector.defaultArguments();
		arguments.get("hostname").setValue("localhost");
		arguments.get("port").setValue("8700");
		VirtualMachine vm = connector.attach(arguments);
		return vm;
	}
}
