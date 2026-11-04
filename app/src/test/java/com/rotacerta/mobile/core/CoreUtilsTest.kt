package com.rotacerta.mobile.core

import com.rotacerta.mobile.core.designsystem.initialsOf
import com.rotacerta.mobile.core.location.Geo
import com.rotacerta.mobile.core.network.parseDrfError
import com.rotacerta.mobile.core.util.Formatters
import com.rotacerta.mobile.core.util.PasswordRules
import com.rotacerta.mobile.core.util.PasswordStrength
import com.rotacerta.mobile.core.util.PolylineDecoder
import com.rotacerta.mobile.core.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class GeoTest {
    @Test
    fun `distancia bate com o backend (haversine)`() {
        // ~556 m: 0,005 grau de latitude perto do equador.
        val d = Geo.distanceMeters(-3.100, -60.0, -3.095, -60.0)
        assertEquals(556.0, d, 2.0)
        assertEquals(0.0, Geo.distanceMeters(-3.1, -60.0, -3.1, -60.0), 0.0)
    }

    @Test
    fun `formata metros e quilometros`() {
        assertEquals("850 m", Geo.formatDistance(847.0))
        assertEquals("2,4 km", Geo.formatDistance(2_400.0))
    }

    @Test
    fun `eta so com o onibus em movimento`() {
        assertNull(Geo.etaMinutes(1_000.0, null))
        assertNull(Geo.etaMinutes(1_000.0, 0.5))
        assertEquals(2, Geo.etaMinutes(1_000.0, 8.0))
        assertEquals(1, Geo.etaMinutes(10.0, 10.0))
    }
}

class PasswordRulesTest {
    @Test
    fun `exige os cinco criterios`() {
        assertTrue(PasswordRules.isValid("Senha@123"))
        assertFalse(PasswordRules.isValid("senha@123"))
        assertFalse(PasswordRules.isValid("Senha1234"))
        assertFalse(PasswordRules.isValid("Se@1"))
    }

    @Test
    fun `forca cresce com os criterios`() {
        assertEquals(PasswordStrength.WEAK, PasswordRules.strength("abc"))
        assertEquals(PasswordStrength.MEDIUM, PasswordRules.strength("abcdefgh1"))
        assertEquals(PasswordStrength.GOOD, PasswordRules.strength("Abcdefgh1"))
        assertEquals(PasswordStrength.STRONG, PasswordRules.strength("Abcdefgh1!"))
    }

    @Test
    fun `valida email e telefone`() {
        assertTrue(Validators.isValidEmail("e001@acme.com"))
        assertTrue(Validators.isValidEmail(" ana.souza@polodigital.com.br "))
        assertFalse(Validators.isValidEmail("ana@"))
        assertTrue(Validators.isValidPhone("(92) 99999-0000"))
        assertTrue(Validators.isValidPhone("+5592999990000"))
        assertFalse(Validators.isValidPhone("1234"))
    }
}

class FormattersTest {
    @Test
    fun `data curta de rota avulsa`() {
        assertEquals("30/10", Formatters.isoDateShort("2026-10-30"))
        assertEquals("amanhã", Formatters.isoDateShort("amanhã"))
    }

    @Test
    fun `dias da semana`() {
        assertEquals("Seg a Sex", Formatters.weekdays(listOf("MON", "TUE", "WED", "THU", "FRI")))
        assertEquals("Seg, Qua", Formatters.weekdays(listOf("WED", "MON")))
        assertEquals("Todos os dias", Formatters.weekdays(listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")))
        assertEquals("", Formatters.weekdays(emptyList()))
    }

    @Test
    fun `tempo relativo`() {
        val now = OffsetDateTime.parse("2026-10-27T12:00:00Z")
        assertEquals("agora", Formatters.relativeTime("2026-10-27T11:59:40Z", now))
        assertEquals("há 15 min", Formatters.relativeTime("2026-10-27T11:45:00Z", now))
        assertEquals("há 3 h", Formatters.relativeTime("2026-10-27T09:00:00Z", now))
        assertEquals("ontem", Formatters.relativeTime("2026-10-26T10:00:00Z", now))
        assertEquals("", Formatters.relativeTime("invalido", now))
    }

    @Test
    fun `nomes`() {
        assertEquals("Ana Souza", Formatters.shortName("Ana Souza Lima"))
        assertEquals("Ponto A", Formatters.beforeDash("Ponto A - Rua X, 10"))
        assertEquals("AS", initialsOf("ana souza lima"))
        assertEquals("?", initialsOf("  "))
    }
}

class DrfErrorTest {
    @Test
    fun `extrai detail, campo e non_field_errors`() {
        assertEquals("Credenciais inválidas.", parseDrfError("""{"detail":"Credenciais inválidas."}"""))
        assertEquals("new_password: Senha muito curta.", parseDrfError("""{"new_password":["Senha muito curta."]}"""))
        assertEquals("Informe um identificador.", parseDrfError("""{"non_field_errors":["Informe um identificador."]}"""))
        assertNull(parseDrfError(""))
        assertNull(parseDrfError("<html>502</html>"))
    }
}

class PolylineDecoderTest {
    @Test
    fun `decodifica o exemplo da documentacao do Google`() {
        val points = PolylineDecoder.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, 1e-5)
        assertEquals(-120.2, points[0].longitude, 1e-5)
        assertEquals(43.252, points[2].latitude, 1e-5)
        assertEquals(-126.453, points[2].longitude, 1e-5)
        assertTrue(PolylineDecoder.decode("").isEmpty())
    }
}
