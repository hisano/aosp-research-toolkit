package jp.hisano.aosp_research_toolkit;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;

public final class ShowCallHistoryOfSystemServices {
	public static void main(String... args) throws IOException {
		VirtualMachine vm;
		try {
			vm = DebuggerUtils.attachToDebuggee();
		} catch (DebuggerException e) {
			System.err.println("Please, select 'system_process' process in 'Devices' view to prepare 8700 debuggee port and restart.");
			System.exit(1);
			return;
		}
		DebuggerUtils.addClassFilters(vm, "com.android.server.am.*");

		ExecutorService worker = Executors.newSingleThreadExecutor();
		worker.submit(() -> {
			EventQueue queue = vm.eventQueue();
			while (true) {
				try {
					EventSet events = queue.remove();
					try {
						events.stream().forEach(event -> {
							if (event instanceof MethodEntryEvent) {
								onMethodEnter((MethodEntryEvent) event);
							}
						});
					} finally {
						events.resume();
					}
					// The following line is required because 'remove' method never throw 'InterruptedException'
					if (Thread.interrupted()) {
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		System.out.println("Recording started. Please, press key to stop.");
		System.in.read();

		worker.shutdown();
		System.exit(0);
	}

	private static final SimpleDateFormat DATE_FORMATER = new SimpleDateFormat("HH:mm:ss.SSS");

	private static void onMethodEnter(MethodEntryEvent event) {
		ThreadReference thread = event.thread();
		Method method = event.method();

		String methodName = method.declaringType().name() + "." + method.name();
		String methodLocation = "";
		{
			try {
				Location location = method.location();
				if (location.lineNumber() != -1) {
					methodLocation = "(" + location.sourceName() + ":" + location.lineNumber() + ")";
				}
			} catch (AbsentInformationException e) {
				// ignore exception
			}
		}

		LinkedHashMap<String, String> arguments = new LinkedHashMap<>();
		try {
			// Use variables method because arguments method don't return arguments which are used as local variables
			method.variables().forEach(argument -> {
				try {
					Value value = thread.frame(0).getValue(argument);
					if (value instanceof BooleanValue) {
						BooleanValue realValue = (BooleanValue)value;
						arguments.put(argument.name(), "" + realValue.booleanValue());
					} else if (value instanceof ByteValue) {
						ByteValue realValue = (ByteValue)value;
						arguments.put(argument.name(), "" + realValue.byteValue());
					} else if (value instanceof CharValue) {
						CharValue realValue = (CharValue)value;
						arguments.put(argument.name(), "" + realValue.charValue());
					} else if (value instanceof ShortValue) {
						ShortValue realValue = (ShortValue)value;
						arguments.put(argument.name(), "" + realValue.shortValue());
					} else if (value instanceof IntegerValue) {
						IntegerValue realValue = (IntegerValue)value;
						arguments.put(argument.name(), "" + realValue.intValue());
					} else if (value instanceof LongValue) {
						LongValue realValue = (LongValue)value;
						arguments.put(argument.name(), "" + realValue.longValue());
					} else if (value instanceof FloatValue) {
						FloatValue realValue = (FloatValue)value;
						arguments.put(argument.name(), "" + realValue.floatValue());
					} else if (value instanceof DoubleValue) {
						DoubleValue realValue = (DoubleValue)value;
						arguments.put(argument.name(), "" + realValue.doubleValue());
					} else if (value instanceof StringReference) {
						StringReference realValue = (StringReference)value;
						arguments.put(argument.name(), "\"" + realValue.value() + "\"");
					} else if (value instanceof ObjectReference) {
						ObjectReference realValue = (ObjectReference)value;
						arguments.put(argument.name(), "<" + realValue.uniqueID() + ">");
					} else {
						arguments.put(argument.name(), "<OTHER>");
					}
				} catch (IncompatibleThreadStateException | IllegalArgumentException e) {
					// skip because this variable is a local variable
					// arguments.put(argument.name(), "<INVALID>");
				}
			});
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		}
		String methodArguments = Joiner.on(", ").join(arguments.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).toArray());

		String time = DATE_FORMATER.format(new Date());
		try {
			System.out.println(time + " | Thread(" + thread.uniqueID() + ") | " + Strings.repeat("+", thread.frameCount() - 1) + " " + methodName + "(" + methodArguments + ") | " + methodLocation);
		} catch (IncompatibleThreadStateException e) {
			System.out.println(thread.uniqueID() + ": " + methodName + "(" + methodArguments + "): " + methodLocation);
		}
	}
}
