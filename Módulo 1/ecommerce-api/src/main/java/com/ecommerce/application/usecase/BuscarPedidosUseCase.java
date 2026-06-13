package com.ecommerce.application.usecase;

import com.ecommerce.application.dto.PedidoResponseDTO;
import com.ecommerce.application.mapper.PedidoMapper;
import com.ecommerce.domain.exception.PedidoNaoEncontradoException;
import com.ecommerce.domain.model.enums.StatusPedido;
import com.ecommerce.domain.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuscarPedidosUseCase {

    private final PedidoRepository pedidoRepository;
    private final PedidoMapper pedidoMapper;

    @Transactional(readOnly = true)
    public PedidoResponseDTO porId(Long id) {
        return pedidoRepository.findById(id)
                .map(pedidoMapper::toResponseDTO)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listar(StatusPedido status, Pageable pageable) {
        Page<?> pagina = (status != null)
                ? pedidoRepository.findByStatus(status, pageable)
                : pedidoRepository.findAll(pageable);

        return pagina.map(p -> pedidoMapper.toResponseDTO((com.ecommerce.domain.model.Pedido) p));
    }
}
