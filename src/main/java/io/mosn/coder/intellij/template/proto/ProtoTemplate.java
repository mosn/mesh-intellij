package io.mosn.coder.intellij.template.proto;

import io.mosn.coder.intellij.option.Configuration;
import io.mosn.coder.intellij.option.Metadata;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProtoTemplate extends AbstractProtocolTemplate<ProtocolOption> {

    @Override
    public Source api(ProtocolOption option) {
        return delegateTemplate(option).api(option);
    }

    @Override
    public Source command(ProtocolOption option) {
        return delegateTemplate(option).command(option);
    }

    @Override
    public Source decoder(ProtocolOption option) {
        return delegateTemplate(option).decoder(option);
    }

    @Override
    public Source encoder(ProtocolOption option) {
        return delegateTemplate(option).encoder(option);
    }

    @Override
    public Source mapping(ProtocolOption option) {
        return delegateTemplate(option).mapping(option);
    }

    @Override
    public Source matcher(ProtocolOption option) {
        return delegateTemplate(option).matcher(option);
    }

    @Override
    public Source protocol(ProtocolOption option) {
        return delegateTemplate(option).protocol(option);
    }

    @Override
    public Source types(ProtocolOption option) {
        return delegateTemplate(option).types(option);
    }

    @Override
    public Source buffer(ProtocolOption option) {
        return delegateTemplate(option).buffer(option);
    }

    @Override
    public Source codec(ProtocolOption option) {
        return delegateTemplate(option).codec(option);
    }

    @Override
    public List<Configuration> configurations(ProtocolOption option) {
        return delegateTemplate(option).configurations(option);
    }

    @Override
    public Metadata metadata(ProtocolOption option) {
        return delegateTemplate(option).metadata(option);
    }

    @NotNull
    private AbstractProtocolTemplate delegateTemplate(ProtocolOption option) {
        AbstractProtocolTemplate delegate = option.isStringRequestId()
                ? new StringIdTemplate() : new StandardTemplate();
        return delegate;
    }
}
