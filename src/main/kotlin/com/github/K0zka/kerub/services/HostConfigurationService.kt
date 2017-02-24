package com.github.K0zka.kerub.services

import com.github.K0zka.kerub.model.config.HostConfiguration
import com.github.K0zka.kerub.security.admin
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresRoles
import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRoles(admin)
@RequiresAuthentication
@Path("host-cfg")
interface HostConfigurationService : RestOperations.Read<HostConfiguration>