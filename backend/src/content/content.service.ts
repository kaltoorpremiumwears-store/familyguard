import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';
import { UpdateWebSafetyDto } from './dto/update-web-safety.dto';
import { AddWebsiteRuleDto } from './dto/website-rule.dto';

@Injectable()
export class ContentService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly eventsGateway: EventsGateway,
  ) {}

  /**
   * Retrieves web safety configuration and domain rules for a child.
   */
  async getWebSafety(parentId: string, childId: string) {
    await this.verifyParentChild(parentId, childId);

    const config = await this.prisma.webSafetyConfig.upsert({
      where: { childId },
      update: {},
      create: {
        childId,
        safeSearchEnabled: true,
        blockAdultContent: true,
        blockGambling: true,
        blockViolence: true,
        blockMalicious: true,
        unapprovedBrowsersBlocked: true,
        youtubeMode: 'SAFETUBE',
        dailyYouTubeLimitMinutes: 60,
      },
    });

    const websiteRules = await this.prisma.websiteRestriction.findMany({
      where: { childId },
      orderBy: { createdAt: 'desc' },
    });

    return {
      config,
      websiteRules,
      managedChromePolicy: this.buildManagedChromePolicy(config, websiteRules),
    };
  }

  /**
   * Updates web safety configuration toggles.
   */
  async updateWebSafety(parentId: string, childId: string, dto: UpdateWebSafetyDto) {
    await this.verifyParentChild(parentId, childId);

    const config = await this.prisma.webSafetyConfig.upsert({
      where: { childId },
      update: dto,
      create: {
        childId,
        safeSearchEnabled: dto.safeSearchEnabled ?? true,
        blockAdultContent: dto.blockAdultContent ?? true,
        blockGambling: dto.blockGambling ?? true,
        blockViolence: dto.blockViolence ?? true,
        blockMalicious: dto.blockMalicious ?? true,
        unapprovedBrowsersBlocked: dto.unapprovedBrowsersBlocked ?? true,
        youtubeMode: dto.youtubeMode ?? 'SAFETUBE',
        dailyYouTubeLimitMinutes: dto.dailyYouTubeLimitMinutes ?? 60,
      },
    });

    const websiteRules = await this.prisma.websiteRestriction.findMany({
      where: { childId },
    });

    const chromePolicy = this.buildManagedChromePolicy(config, websiteRules);

    // Real-Time Push to child device
    this.eventsGateway.broadcastPolicyUpdate(childId, {
      webSafety: config,
      managedChromePolicy: chromePolicy,
    });

    return {
      message: 'Web safety configuration updated successfully.',
      config,
      managedChromePolicy: chromePolicy,
    };
  }

  /**
   * Adds or updates a domain rule (block or whitelist).
   */
  async addWebsiteRule(parentId: string, childId: string, dto: AddWebsiteRuleDto) {
    await this.verifyParentChild(parentId, childId);

    const domain = dto.domain.toLowerCase().trim().replace(/^https?:\/\//, '').replace(/\/.*$/, '');

    const rule = await this.prisma.websiteRestriction.upsert({
      where: {
        childId_domain: {
          childId,
          domain,
        },
      },
      update: {
        isBlocked: dto.isBlocked,
        category: dto.category || 'CUSTOM',
      },
      create: {
        childId,
        domain,
        isBlocked: dto.isBlocked,
        category: dto.category || 'CUSTOM',
      },
    });

    // Broadcast updated rules
    const config = await this.prisma.webSafetyConfig.findUnique({ where: { childId } });
    const allRules = await this.prisma.websiteRestriction.findMany({ where: { childId } });
    if (config) {
      this.eventsGateway.broadcastPolicyUpdate(childId, {
        managedChromePolicy: this.buildManagedChromePolicy(config, allRules),
      });
    }

    return {
      message: `Domain ${domain} ${dto.isBlocked ? 'blocked' : 'whitelisted'} successfully.`,
      rule,
    };
  }

  /**
   * Deletes a domain rule.
   */
  async deleteWebsiteRule(parentId: string, childId: string, ruleId: string) {
    await this.verifyParentChild(parentId, childId);

    await this.prisma.websiteRestriction.delete({
      where: { id: ruleId },
    });

    return { message: 'Website rule removed.' };
  }

  /**
   * Generates native Android Enterprise App Restriction Bundle for Google Chrome.
   * This is sent to the child DPC to execute:
   * dpm.setApplicationRestrictions(admin, "com.android.chrome", bundle)
   */
  buildManagedChromePolicy(config: any, websiteRules: any[]) {
    const blockList: string[] = [];
    const allowList: string[] = [];

    // Custom blocked domains
    websiteRules.forEach((r) => {
      if (r.isBlocked) {
        blockList.push(r.domain);
        blockList.push(`*.${r.domain}`);
      } else {
        allowList.push(r.domain);
        allowList.push(`*.${r.domain}`);
      }
    });

    // High-risk domain patterns
    if (config.blockAdultContent) {
      blockList.push('*porn*', '*adult*', '*xxx*', '*sex*');
    }
    if (config.blockGambling) {
      blockList.push('*casino*', '*bet*', '*poker*', '*gambling*');
    }

    return {
      packageName: 'com.android.chrome',
      restrictions: {
        URLBlocklist: blockList,
        URLAllowlist: allowList,
        ForceGoogleSafeSearch: config.safeSearchEnabled,
        ForceYouTubeRestrict: 2, // 2 = Strict YouTube Restricted Mode
        IncognitoModeAvailability: 1, // 1 = Disabled incognito
        BrowserSignin: 0, // Disable bypassing with secondary personal accounts
      },
    };
  }

  private async verifyParentChild(parentId: string, childId: string) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
    });

    if (!child) {
      throw new NotFoundException('Child profile not found.');
    }

    if (child.parentId !== parentId) {
      throw new ForbiddenException('Unauthorized.');
    }

    return child;
  }
}
