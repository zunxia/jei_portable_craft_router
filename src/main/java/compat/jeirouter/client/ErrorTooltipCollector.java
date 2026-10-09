package compat.jeirouter.client;

import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiKeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

final class ErrorTooltipCollector implements ITooltipBuilder {
    private final List<Either<FormattedText, TooltipComponent>> lines = new ArrayList<>();
    public void add(FormattedText text) { lines.add(Either.left(text)); }
    public void addAll(java.util.Collection<? extends FormattedText> texts) { texts.forEach(this::add); }
    public void add(TooltipComponent component) { lines.add(Either.right(component)); }
    public void addKeyUsageComponent(String key, IJeiKeyMapping mapping) { add(Component.translatable(key)); }
    public void setIngredient(ITypedIngredient<?> ingredient) {}
    public void clearIngredient() {}
    public void clear() { lines.clear(); }
    public List<Either<FormattedText, TooltipComponent>> getLines() { return List.copyOf(lines); }
    public List<Component> toLegacyToComponents() {
        return lines.stream().flatMap(l -> l.left().stream()).filter(Component.class::isInstance).map(Component.class::cast).toList();
    }
    public void removeAll(List<Component> values) { lines.removeIf(l -> l.left().map(values::contains).orElse(false)); }
}
