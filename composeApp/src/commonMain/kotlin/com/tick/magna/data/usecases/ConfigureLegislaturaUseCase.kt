package com.tick.magna.data.usecases

import com.tick.magna.data.logger.AppLoggerInterface
import com.tick.magna.data.repository.LegislaturaRepositoryInterface
import com.tick.magna.data.source.local.dao.UserDaoInterface

class ConfigureLegislaturaUseCase(
    private val userDao: UserDaoInterface,
    private val legislaturaRepository: LegislaturaRepositoryInterface,
    private val logger: AppLoggerInterface,
) {
    companion object Companion {
        private const val TAG = "ConfigureLegislaturaUseCase"
    }

    suspend operator fun invoke(legislaturaStartDate: String) {
        logger.d("ConfigureLegislaturaUseCase -> $legislaturaStartDate", TAG)
        val legislatura = legislaturaRepository.getLegislatura(legislaturaStartDate)
        if (legislatura != null) {
            userDao.setUserLegislatura(legislatura.id)
        } else {
            logger.d("ConfigureLegislaturaUseCase -> Couldn't fetch legislatura", TAG)
        }
    }
}
