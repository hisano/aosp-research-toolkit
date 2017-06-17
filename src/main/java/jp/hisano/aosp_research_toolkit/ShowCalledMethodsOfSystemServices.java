package jp.hisano.aosp_research_toolkit;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.Sets;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;

public final class ShowCalledMethodsOfSystemServices {
	public static void main(String... args) throws IOException {
		VirtualMachine vm;
		try {
			vm = DebuggerUtils.attachToDebuggee();
		} catch (DebuggerException e) {
			System.err.println("Please, select 'system_process' process in 'Devices' view to prepare 8700 debuggee port and restart.");
			System.exit(1);
			return;
		}
		DebuggerUtils.addClassFilterForSystemServices(vm);

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
}
