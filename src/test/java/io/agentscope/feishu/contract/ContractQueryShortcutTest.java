package io.agentscope.feishu.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentscope.feishu.contract.dto.ContractInfoResponse;
import io.agentscope.feishu.lark.FeishuMessageSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractQueryShortcutTest {

    @Mock
    private ContractRemoteClient contractRemoteClient;

    @Mock
    private FeishuMessageSender messageSender;

    @Mock
    private ContractProperties contractProperties;

    @Mock
    private ContractReplyFormatter contractReplyFormatter;

    @InjectMocks
    private ContractQueryShortcut shortcut;

    @Test
    void phraseSendsTemplateCard() throws Exception {
        when(contractProperties.isUseTemplateCard()).thenReturn(true);
        when(contractProperties.getCardTemplateId()).thenReturn("AAqtmy18CRaGt");
        var dto = new ContractInfoResponse("HT-20250908192882", "n", "a", "b", "c", "2025-09-10");
        when(contractRemoteClient.fetchContractInfo("HT-20250908192882")).thenReturn(dto);
        when(messageSender.sendContractTemplateCard("cid", dto)).thenReturn(true);

        assertTrue(shortcut.tryHandle("cid", "查询HT-20250908192882合同"));
        verify(messageSender).sendContractTemplateCard(eq("cid"), eq(dto));
        verify(messageSender, never()).replyTextToChat(any(), any());
    }

    @Test
    void nonPhraseReturnsFalse() throws Exception {
        assertFalse(shortcut.tryHandle("cid", "查合同"));
        verify(contractRemoteClient, never()).fetchContractInfo(any());
    }
}
