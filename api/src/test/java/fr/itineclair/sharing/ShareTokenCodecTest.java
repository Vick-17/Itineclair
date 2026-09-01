package fr.itineclair.sharing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShareTokenCodecTest {

    @Test
    void generatesUrlSafeHighEntropyTokensAndOnlyKeepsTheirHash() {
        ShareTokenCodec codec = new ShareTokenCodec();

        ShareTokenCodec.TokenMaterial first = codec.generate();
        ShareTokenCodec.TokenMaterial second = codec.generate();

        assertThat(first.token())
                .hasSize(43)
                .matches("[A-Za-z0-9_-]{43}")
                .isNotEqualTo(second.token());
        assertThat(first.hash())
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .doesNotContain(first.token());
        assertThat(codec.hashPresented(first.token()))
                .contains(first.hash());
    }

    @Test
    void rejectsMalformedPresentedTokensBeforeHashing() {
        ShareTokenCodec codec = new ShareTokenCodec();

        assertThat(codec.hashPresented(null)).isEmpty();
        assertThat(codec.hashPresented("too-short")).isEmpty();
        assertThat(codec.hashPresented("A".repeat(42) + "."))
                .isEmpty();
    }
}
