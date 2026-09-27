package codelab.client;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.msgpack.value.Value;
import org.msgpack.value.MapValue;
import org.msgpack.value.ArrayValue;
import org.msgpack.core.MessageUnpacker;

/**
*	Commandes reçues du serveur
*	@author Jérôme Lehuen
*	@version 10/05/25
*/

public class Command {

	public String name;
	public int nbargs;
	public Object[] args;

	public void describe() {
		System.out.printf("CLIENT: Received command %s with %d args:%n", name, nbargs);
		for (int i = 0; i < nbargs; i++) {
			Object arg = args[i];
			if (arg != null) {
				System.out.printf("	  Argument %d: type=%s value=%s%n", i, arg.getClass().getTypeName(), arg);
			}
		}
	}

	///////////////////////////////////////////////////
	// Constructeur et méthodes associées
	///////////////////////////////////////////////////

	public Command(MessageUnpacker unpacker) throws Exception {
		unpacker.unpackArrayHeader();

		// Récupération de la commande
		Value cmdValue = unpacker.unpackValue();
		name = cmdValue.asStringValue().asString();

		// Récupération du nombre d'arguments
		Value nbargsValue = unpacker.unpackValue();
		nbargs = nbargsValue.asIntegerValue().asInt();

		// Récupération des arguments
		Value argsValue = unpacker.unpackValue();
		args = convertArrayToJava(argsValue.asArrayValue());
	}

	// ----------------------------------------------------
	// Conversion des types MessagePack en types Java

	private Object convertValueToJava(Value value) {
		switch (value.getValueType()) {
			case STRING: return value.asStringValue().asString();
			case INTEGER: return value.asIntegerValue().asInt();
			case BOOLEAN: return value.asBooleanValue().getBoolean();
			case BINARY: return value.asBinaryValue().asByteArray();
			case FLOAT: return value.asFloatValue().toDouble();
			case ARRAY: return convertArrayToJava(value.asArrayValue());
			case MAP: return convertMapToJava(value.asMapValue());
			default: throw new IllegalArgumentException("Unsupported MessagePack type: " + value.getValueType());
		}
	}

	// ----------------------------------------------------
	// Conversion des types ArrayValue et MapValue

	private Object[] convertArrayToJava(ArrayValue arrayValue) {
		List<Object> list = new ArrayList<>();
		for (Value element : arrayValue) {
			list.add(convertValueToJava(element));
		}
		return list.toArray();
	}

	private Map<String, Object> convertMapToJava(MapValue mapValue) {
		Map<String, Object> map = new HashMap<>();
		for (Map.Entry<Value, Value> entry : mapValue.entrySet()) {
			map.put(
				entry.getKey().asStringValue().asString(),
				convertValueToJava(entry.getValue()));
		}
		return map;
	}
}
