package org.antlr.intellij.plugin.configdialogs;

import com.intellij.util.xmlb.Converter;
import org.antlr.intellij.plugin.parsing.CaseChangingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A converter used to serialize/deserialize a {@code CaseChangingStrategy} to/from
 * a String contained in {@code .idea/misc.xml} using the enum's {@code name()}.
 */
public class CaseChangingStrategyConverter extends Converter<CaseChangingStrategy> {
	@Nullable
	@Override
	public CaseChangingStrategy fromString(@NotNull String value) {
		try {
			return CaseChangingStrategy.valueOf(value);
		} catch ( IllegalArgumentException e ) {
			return null;
		}
	}

	@Nullable
	@Override
	public String toString(@NotNull CaseChangingStrategy value) {
		return value.name();
	}
}
